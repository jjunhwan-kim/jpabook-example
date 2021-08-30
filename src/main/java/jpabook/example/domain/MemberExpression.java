package jpabook.example.domain;

import com.querydsl.core.annotations.QueryDelegate;
import com.querydsl.core.types.dsl.BooleanExpression;

public class MemberExpression {

    @QueryDelegate(Member.class)
    public static BooleanExpression isOlderThan(QMember member, Integer age) {
        return member.age.gt(age);
    }
}
