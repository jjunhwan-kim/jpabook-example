package jpabook.example;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.NonUniqueResultException;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jpabook.example.domain.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static jpabook.example.domain.QMember.*;
import static jpabook.example.domain.QMember.member;
import static jpabook.example.domain.QTeam.team;
import static jpabook.example.domain.QUser.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@Rollback(false)
public class QuerydslTest {

    static boolean isLoaded = false;

    @PersistenceContext
    EntityManager em;

    @Autowired JPAQueryFactory queryFactory;

    @BeforeEach
    void init() {
        if (!isLoaded) {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            Member member1 = new Member("member1", 10, teamA);
            Member member2 = new Member("member2", 20, teamA);
            Member member3 = new Member("member3", 30, teamB);
            Member member4 = new Member("member4", 30, teamB);
            em.persist(member1);
            em.persist(member2);
            em.persist(member3);
            em.persist(member4);

            em.flush();
            em.clear();

            isLoaded = true;
        }
    }

    @Test
    void querydsl() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em); // JPAQueryFactory 생성자 파라미터로 EntityManager를 전달
        QMember qMember = new QMember("m"); // 생성되는 JPQL의 별칭 m
        List<Member> members = queryFactory.selectFrom(qMember)
                .where(qMember.name.eq("member"))
                .orderBy(qMember.name.desc())
                .fetch();
    }

    @Test
    void basic() {
        List<Member> members = queryFactory.selectFrom(member)
                .where(member.name.eq("member"))
                .orderBy(member.name.desc())
                .fetch();
    }

    @Test
    void search1() {
        Member foundMember = queryFactory.selectFrom(member)
                .where(member.name.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        assertThat(foundMember.getName()).isEqualTo("member1");
        assertThat(foundMember.getAge()).isEqualTo(10);
    }

    @Test
    void search2() {
        Member foundMember = queryFactory.selectFrom(member)
                .where(member.name.eq("member1"), member.age.eq(10))
                .fetchOne();

        assertThat(foundMember.getName()).isEqualTo("member1");
        assertThat(foundMember.getAge()).isEqualTo(10);
    }

    @Test
    void search3() {
        member.age.in(10, 20);      // age in (10, 20)
        member.age.notIn(10, 20);   // age not in (10, 20)
        member.age.between(10, 20);           // between 10, 20

        member.age.goe(10);             // age >= 10
        member.age.gt(10);              // age > 10
        member.age.loe(10);             // age <= 10
        member.age.lt(10) ;             // age < 10

        member.name.eq("member");
        member.name.ne("member");
        member.name.like("member");
        member.name.contains("member");
        member.name.startsWith("member");
    }

    @Test
    void result() {
        List<Member> members = queryFactory.selectFrom(member)
                .fetch();

        User user = queryFactory.selectFrom(QUser.user)
                .fetchOne();

        assertThat(user).isNull();

        assertThrows(NonUniqueResultException.class, () -> queryFactory.selectFrom(member)
                .fetchOne());

        Member firstMember = queryFactory.selectFrom(member).fetchFirst();

        long count = queryFactory.selectFrom(member).fetchCount();
        assertThat(count).isEqualTo(4);

        QueryResults<Member> memberQueryResults = queryFactory.selectFrom(member).fetchResults();

        List<Member> results = memberQueryResults.getResults();
        assertThat(results).isEqualTo(members);

        long total = memberQueryResults.getTotal();
        assertThat(total).isEqualTo(4);

        long offset = memberQueryResults.getOffset();
        long limit = memberQueryResults.getLimit();
    }

    @Test
    void sort() {
        List<Member> members = queryFactory.selectFrom(member)
                .orderBy(member.age.desc(), member.name.asc())
                .fetch();

        Assertions.assertThat(members.get(0).getName()).isEqualTo("member3");
        Assertions.assertThat(members.get(1).getName()).isEqualTo("member4");
        Assertions.assertThat(members.get(2).getName()).isEqualTo("member2");
        Assertions.assertThat(members.get(3).getName()).isEqualTo("member1");
    }

    @Test
    void paging() {
        List<Member> members = queryFactory.selectFrom(member)
                .orderBy(member.name.desc())
                .offset(1)
                .limit(2)
                .fetch();
        Assertions.assertThat(members.get(0).getName()).isEqualTo("member3");
        Assertions.assertThat(members.get(1).getName()).isEqualTo("member2");
    }

    @Test
    void aggregation() {
        List<Tuple> result = queryFactory.select(member.count(), member.age.sum(), member.age.avg(), member.age.max(), member.age.min())
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(90);
        assertThat(tuple.get(member.age.avg())).isEqualTo(90.0/4.0);
        assertThat(tuple.get(member.age.max())).isEqualTo(30);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @Test
    void group() {
        List<Tuple> result = queryFactory.select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(member.team)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(30);
    }

    @Test
    void join() {
        List<Member> members = queryFactory.selectFrom(member)
                .join(member.team, team)
                .fetch();
    }

    @Test()
    void thetaJoin() {
        em.persist(new Team("member1"));
        em.persist(new Team("member2"));

        List<Tuple> result = queryFactory.select(member, team)
                .from(member, team)
                .where(member.name.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test()
    void joinOn() {
        List<Member> members = queryFactory.select(member)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();
    }

    @Test()
    void joinOnNoRelation() {
        em.persist(new Team("member1"));
        em.persist(new Team("member2"));

        List<Tuple> result1 = queryFactory.select(member, team)
                .from(member)
                .leftJoin(team).on(member.name.eq(team.name))
                .fetch();

        for (Tuple tuple : result1) {
            System.out.println("tuple = " + tuple);
        }

        List<Tuple> result2 = queryFactory.select(member, team)
                .from(member)
                .join(team).on(member.name.eq(team.name))
                .fetch();

        for (Tuple tuple : result2) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void fetchJoin() {
        Member foundMember = queryFactory.selectFrom(member)
                .join(member.team).fetchJoin()
                .where(member.name.eq("member1"))
                .fetchOne();

        assertThat(foundMember.getTeam().getName()).isEqualTo("teamA");
    }

    @Test
    void subQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Member> members = queryFactory.selectFrom(QMember.member)
                .where(QMember.member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)))
                .fetch();
        assertThat(members.size()).isEqualTo(2);
        assertThat(members.get(0).getAge()).isEqualTo(30);
        assertThat(members.get(1).getAge()).isEqualTo(30);
    }

    @Test
    void subQuerySelect() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory.select(member.name, JPAExpressions.select(memberSub.age.avg()).from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String name = tuple.get(member.name);
            Double age = tuple.get(JPAExpressions.select(memberSub.age.avg()).from(memberSub));
            System.out.println("name = " + name + " age = " + age);
        }
    }

    @Test
    void projectionBasic() {
        List<String> memberNames = queryFactory.select(member.name)
                .from(member)
                .fetch();
    }

    @Test
    void projectionTuple() {
        List<Tuple> result = queryFactory.select(member.name, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String name = tuple.get(member.name);
            Integer age = tuple.get(member.age);
            System.out.println("name = " + name);
            System.out.println("age = " + age);
        }
    }

    @Test
    void projectionJpaDto() {
        List<MemberDto> memberDtos = em.createQuery("select new jpabook.example.domain.MemberDto(m.name, m.age) from Member m", MemberDto.class).getResultList();
        for (MemberDto memberDto : memberDtos) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void projectionDtoProperty() {
        List<MemberDto> memberDtos = queryFactory.select(Projections.bean(MemberDto.class, member.name, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : memberDtos) {
            System.out.println("memberDto = " + memberDto);
        }

        List<UserDto> userDtos = queryFactory.select(Projections.bean(UserDto.class, member.name.as("username"), member.age))
                .from(member)
                .fetch();
        for (UserDto userDto : userDtos) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void projectionDtoField() {
        List<MemberDto> memberDtos = queryFactory.select(Projections.fields(MemberDto.class, member.name, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : memberDtos) {
            System.out.println("memberDto = " + memberDto);
        }

        List<UserDto> userDtos = queryFactory.select(Projections.fields(UserDto.class, member.name.as("username"), member.age))
                .from(member)
                .fetch();
        for (UserDto userDto : userDtos) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void projectionDtoConstructor() {
        List<MemberDto> memberDtos = queryFactory.select(Projections.constructor(MemberDto.class, member.name, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : memberDtos) {
            System.out.println("memberDto = " + memberDto);
        }

        List<UserDto> userDtos = queryFactory.select(Projections.constructor(UserDto.class, member.name, member.age))
                .from(member)
                .fetch();
        for (UserDto userDto : userDtos) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void queryProjection() {
        List<MemberDto> mem = queryFactory.select(new QMemberDto(member.name, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : mem) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void distinct() {
        List<Member> result1 = queryFactory.selectDistinct(member)
                .from(member)
                .fetch();

        List<Member> result2 = queryFactory.selectFrom(member).distinct()
                .from(member)
                .fetch();

        List<Member> result3 = queryFactory.select(member).distinct()
                .from(member)
                .fetch();
    }

    @Test
    void update() {
        long count = queryFactory.update(member)
                .set(member.name, "none")
                .where(member.age.goe(30))
                .execute();
    }

    @Test
    void delete() {
        long count = queryFactory.delete(member)
                .where(member.age.goe(30))
                .execute();
    }

    @Test
    void dynamicQueryBooleanBuilder() {
        List<Member> result1 = searchMember("member1", 10);
        List<Member> result2 = searchMember("member1", null);
    }

    private List<Member> searchMember(String name, Integer age) {
        BooleanBuilder builder = new BooleanBuilder();
        if (name != null) {
            builder.and(member.name.eq(name));
        }
        if (age != null) {
            builder.and(member.age.eq(age));
        }
        return queryFactory.selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    void dynamicQueryWhere() {
        List<Member> result1 = searchMember2("member1", 10);
        List<Member> result2 = searchMember2("member1", null);
    }

    private List<Member> searchMember2(String name, Integer age) {
        return queryFactory.selectFrom(member)
                .where(nameEq(name), ageEq(age))
                .fetch();
    }

    private BooleanExpression nameEq(String name) {
        return name != null ? member.name.eq(name) : null;
    }

    private BooleanExpression ageEq(Integer age) {
        return age != null ? member.age.eq(age) : null;
    }

    @Test
    void delegateMethod() {
        List<Member> members = queryFactory.selectFrom(member)
                .where(member.isOlderThan(20))
                .fetch();
        System.out.println("members = " + members);
    }
}
