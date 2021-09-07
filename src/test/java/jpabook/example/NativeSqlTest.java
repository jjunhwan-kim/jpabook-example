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
import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class NativeSqlTest {

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
    void projectionEntity() {
        // 위치 기반 파라미터
        String sql1 = "select member_id, age, name, team_id from member where age > ?";
        List<Member> result1 = em.createNativeQuery(sql1, Member.class)
                .setParameter(1, 20)
                .getResultList();

        assertThat(result1.size()).isEqualTo(2);
        assertThat(result1).extracting("name").containsExactly("member3", "member4");

        for (Member member : result1) {
            System.out.println("member = " + member);
        }

        // 이름 기반 파라미터
        String sql2 = "select member_id, age, name, team_id from member where age > :age";
        List<Member> result2 = em.createNativeQuery(sql2, Member.class)
                .setParameter("age", 20)
                .getResultList();

        assertThat(result2.size()).isEqualTo(2);
        assertThat(result2).extracting("name").containsExactly("member3", "member4");
        for (Member member : result2) {
            System.out.println("member = " + member);
        }
    }

    @Test
    void projectionValue() {
        String sql = "select member_id, age, name, team_id from member where age > ?";
        List<Object[]> result = em.createNativeQuery(sql)
                .setParameter(1, 10)
                .getResultList();

        assertThat(result).size().isEqualTo(3);

        for (Object[] row : result) {
            System.out.println("member_id = " + row[0]);
            System.out.println("age = " + row[1]);
            System.out.println("name = " + row[2]);
            System.out.println("team_id = " + row[3]);
        }
    }

    @Test
    void resultMapping1() {
        String sql = "select m.member_id, age, name, team_id, i.order_count " +
                "from member m " +
                "left join " +
                "(select im.member_id, count(*) as order_count " +
                "from orders o, member im " +
                "where o.member_id = im.member_id " +
                "group by o.member_id) i " +
                "on m.member_id = i.member_id";

        List<Object[]> result = em.createNativeQuery(sql, "memberWithOrderCount").getResultList();
        for (Object[] row : result) {
            Member member = (Member) row[0];
            BigInteger orderCount = (BigInteger) row[1];

            System.out.println("member = " + member);
            System.out.println("orderCount = " + orderCount);
        }
    }

    @Test
    void resultMapping2() {
        String sql = "select o.order_id as order_id, " +
                "o.quantity as order_quantity, " +
                "o.item_id as order_item, " +
                "o.member_id as member_id, " +
                "i.name as item_name, " +
                "from orders o, item i " +
                "where (o.quantity > 25) and (o.item_id = i.item_id)";

        List<Object[]> result = em.createNativeQuery(sql, "OrderResults").getResultList();
        for (Object[] row : result) {
            Order order = (Order) row[0];
            String itemName = (String) row[1];

            System.out.println("order = " + order);
            System.out.println("itemName = " + itemName);
        }
    }

    @Test
    void namedNativeQuery1() {
        List<Member> result = em.createNamedQuery("Member.memberSQL", Member.class)
                .setParameter(1, 20)
                .getResultList();

        assertThat(result.size()).isEqualTo(2);
        assertThat(result).extracting("name").containsExactly("member3", "member4");

        for (Member member : result) {
            System.out.println("member = " + member);
        }
    }

    @Test
    void namedNativeQuery2() {
        List<Object[]> result = em.createNamedQuery("Member.memberWithOrderCount")
                .getResultList();

        for (Object[] row : result) {
            Member member = (Member) row[0];
            BigInteger orderCount = (BigInteger) row[1];

            System.out.println("member = " + member);
            System.out.println("orderCount = " + orderCount);
        }
    }

    @Test
    void nativeQueryPaging() {
        List<Member> result = em.createNativeQuery("select member_id, age, name, team_id from member", Member.class)
                .setFirstResult(1)
                .setMaxResults(2)
                .getResultList();

        assertThat(result.size()).isEqualTo(2);
        assertThat(result).extracting("name").containsExactly("member2", "member3");
    }
}
