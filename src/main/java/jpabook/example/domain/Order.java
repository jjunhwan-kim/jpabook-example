package jpabook.example.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Getter @Setter
@NoArgsConstructor
@ToString(of = {"id", "quantity"})
@Entity
@Table(name = "orders")
@SqlResultSetMapping(name = "OrderResults",
        entities = {
        @EntityResult(entityClass = Order.class, fields = {
                @FieldResult(name = "id", column = "order_id"),
                @FieldResult(name = "member", column = "member_id"),
                @FieldResult(name = "quantity", column = "order_quantity"),
                @FieldResult(name = "item", column = "order_item")})},
        columns = {@ColumnResult(name = "item_name")}
)
public class Order {

    @Id @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    private int quantity;

    public Order(Member member, Item item, int quantity) {
        this.member = member;
        this.item = item;
        this.quantity = quantity;
    }
}
