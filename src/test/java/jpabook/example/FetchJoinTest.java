package jpabook.example;

import jpabook.example.domain.Member;
import jpabook.example.domain.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

@SpringBootTest
@Transactional
@Rollback(false)
public class FetchJoinTest {

    static boolean isLoaded = false;

    @Autowired
    EntityManager em;

    @BeforeEach
    void init() {
        if (!isLoaded) {
            Team team1 = new Team();
            team1.setName("team1");
            em.persist(team1);

            Team team2 = new Team();
            team2.setName("team2");
            em.persist(team2);

            Team team3 = new Team();
            team3.setName("team3");
            em.persist(team3);

            Team team4 = new Team();
            team4.setName("team4");
            em.persist(team4);

            Team team5 = new Team();
            team5.setName("team5");
            em.persist(team5);

            for (int i = 1; i <= 25; i++) {
                Member member = new Member();
                member.setName("member" + i);
                member.setAge(i);

                if (i <= 5) member.setTeam(team1);
                else if (i <= 10) member.setTeam(team2);
                else if (i <= 15) member.setTeam(team3);
                else if (i <= 20) member.setTeam(team4);
                else member.setTeam(team5);

                em.persist(member);
            }

            em.flush();
            em.clear();

            isLoaded = true;
        }
    }

    /**
     * 일대다 관계, 컬렉션에서 Fetch Join 대상(Member)을 별칭으로 사용하면 안됨
     * Fetch Join 대상(Member)을 필터링하면, Team.memeber의 데이터가 DB의 데이터와 다르게 출력되어 일관성이 깨짐
     */
    @Test
    void fetchJoinCollectionWithSearch() {
        // Member.name이 "member1"인 Team 조회
        List<Team> team = em.createQuery("select t from Team t join fetch t.members m where m.name = :name", Team.class)
                .setParameter("name", "member1")
                .getResultList();

        System.out.println("team.size() = " + team.size());

        // team.members는 5개가 나와야하지만 1개가 나옴
        System.out.println("team.get(0).getMembers().size() = " + team.get(0).getMembers().size());
        for (Member member : team.get(0).getMembers()) {
            System.out.println("member.getName() = " + member.getName());
        }
    }

    /**
     * 일대다 관계, 컬렉션에서 SQL 쿼리 성능을 최적화 하면서 페이징을 할 수 있는 방법
     *
     * 먼저 일대일, 다대일 관계를 모두 페치조인한다.
     * 페이징 처리를 한다.
     * 컬렉션은 지연 로딩으로 조회한다.
     * 지연 로딩 성능 최적화를 위해 hibernate.default_batch_fetch_size, @BatchSize를 적용한다.
     * hibernate.default_batch_fetch_size는 글로벌 설정, @BatchSize는 개별 설정이다.
     * 이 옵션으로 컬렉션이나 프록시 객체를 설정한 size 만큼 IN 쿼리로 조회한다.
     */

    @Test
    void fetchJoinCollectionWithPaging() {

        List<Team> teams = em.createQuery("select t from Team t", Team.class)
                .setFirstResult(1)
                .setMaxResults(10)
                .getResultList();
        // Team 조회 SQL 1번 실행
        // Team.members 조회 SQL은 Team의 개수 N만큼 실행되어야 하지만, batch size 설정으로 batch size 만큼 한 번에 실행함
        System.out.println("teams.size() = " + teams.size());
        for (Team team : teams) {
            System.out.println("team.getName() = " + team.getName());
            for (Member member : team.getMembers()) {
                System.out.println("member.getName() = " + member.getName());
            }
        }
    }

    /**
     *  일대다 관계, 컬렉션에서 페이징 API를 사용하면 안됨
     *  일대다 관계에서 조인하면 데이터베이스의 로우의 수는 다(N)의 데이터 개수로 맞춰지는데, 일(1) 데이터를 기준으로 페이징을 하려고 하니까 문제가 발생함
     *  출력되는 SQL 로그를 보면 페이징 SQL이 나가지 않음
     *  하이버네이트는 애플리케이션에 모든 데이터를 읽어서 페이징 처리함
     *  데이터가 많으면 메모리 초과가 발생할 수 있음
     *  firstResult/maxResults specified with collection fetch; applying in memory! 로그 발생함
     */
    @Test
    void pagingWithCollection() {
        List<Team> teams = em.createQuery("select distinct t from Team t join fetch t.members", Team.class)
                .setFirstResult(1)
                .setMaxResults(10)
                .getResultList();
    }
}
