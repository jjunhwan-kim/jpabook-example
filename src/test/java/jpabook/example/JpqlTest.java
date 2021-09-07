package jpabook.example;

import jpabook.example.domain.Item;
import jpabook.example.domain.Member;
import jpabook.example.domain.Order;
import jpabook.example.domain.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class JpqlTest {

    @Autowired
    EntityManager em;

    @BeforeEach
    void init() {
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

        Item item1 = new Item("item1");
        Item item2 = new Item("item2");
        em.persist(item1);
        em.persist(item2);

        Order order1 = new Order(member1, item1, 30);
        Order order2 = new Order(member1, item2, 20);
        Order order3 = new Order(member2, item1, 30);
        Order order4 = new Order(member2, item2, 30);
        Order order5 = new Order(member3, item1, 30);
        em.persist(order1);
        em.persist(order2);
        em.persist(order3);
        em.persist(order4);
        em.persist(order5);

        em.flush();
        em.clear();
    }

    @Test
    void update() {
        int count = em.createQuery("update Member m set m.age = 40 where m.age >= :age")
                .setParameter("age", 30)
                .executeUpdate();

        assertThat(count).isEqualTo(2);
    }

    @Test
    void delete() {
        int count = em.createQuery("delete from Order o")
                .executeUpdate();

        assertThat(count).isEqualTo(5);
    }

    @Test
    void bulk() {
        Member foundMember = em.createQuery("select m from Member m where m.name = :name", Member.class)
                .setParameter("name", "member1")
                .getSingleResult();

        assertThat(foundMember.getAge()).isEqualTo(10);

        int count = em.createQuery("update Member m set m.age = :age")
                .setParameter("age", 40)
                .executeUpdate();

        System.out.println("====");

        // 벌크 연산은 영속성 컨텍스트를 통하지 않고 데이터베이스에 직접 SQL문을 전송하므로,
        // 영속성 컨텍스트에 있는 회원과 데이터베이스에 있는 회원의 나이가 다를 수 있다.
        assertThat(foundMember.getAge()).isEqualTo(10);
    }
}
