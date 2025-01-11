package kr.hhplus.be.server.domain.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QOrder is a Querydsl query type for Order
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrder extends EntityPathBase<Order> {

    private static final long serialVersionUID = -887003040L;

    public static final QOrder order = new QOrder("order1");

    public final kr.hhplus.be.server.domain.common.entity.QBaseTimeEntity _super = new kr.hhplus.be.server.domain.common.entity.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Integer> discountAmount = createNumber("discountAmount", Integer.class);

    public final NumberPath<Integer> finalAmount = createNumber("finalAmount", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<kr.hhplus.be.server.domain.order.type.OrderStatusType> orderStatus = createEnum("orderStatus", kr.hhplus.be.server.domain.order.type.OrderStatusType.class);

    public final NumberPath<Integer> originalAmount = createNumber("originalAmount", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QOrder(String variable) {
        super(Order.class, forVariable(variable));
    }

    public QOrder(Path<? extends Order> path) {
        super(path.getType(), path.getMetadata());
    }

    public QOrder(PathMetadata metadata) {
        super(Order.class, metadata);
    }

}

