package jpabook.example.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

import static javax.persistence.FetchType.*;

@Setter @Getter
@NoArgsConstructor
@ToString(exclude = "team")
@Entity
@NamedNativeQueries({
        @NamedNativeQuery(
                name = "Member.memberSQL",
                query = "select member_id, age, name, team_id from member where age > ?",
                resultClass = Member.class
        ),
        @NamedNativeQuery(
                name = "Member.memberWithOrderCount",
                query = "select m.member_id, age, name, team_id, i.order_count " +
                        "from member m " +
                        "left join " +
                        "(select im.member_id, count(*) as order_count " +
                        "from orders o, member im " +
                        "where o.member_id = im.member_id " +
                        "group by o.member_id) i " +
                        "on m.member_id = i.member_id",
                resultSetMapping = "memberWithOrderCount"
        )
})
@SqlResultSetMapping(name = "memberWithOrderCount",
        entities = {@EntityResult(entityClass = Member.class)},
        columns = {@ColumnResult(name = "order_count")})
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String name;
    private int age;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public Member(String name, int age, Team team) {
        this.name = name;
        this.age = age;
        this.team = team;
    }
}
