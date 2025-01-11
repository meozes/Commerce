package kr.hhplus.be.server.domain.balance.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBalanceHistory is a Querydsl query type for BalanceHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBalanceHistory extends EntityPathBase<BalanceHistory> {

    private static final long serialVersionUID = -203377260L;

    public static final QBalanceHistory balanceHistory = new QBalanceHistory("balanceHistory");

    public final kr.hhplus.be.server.domain.common.entity.QBaseTimeEntity _super = new kr.hhplus.be.server.domain.common.entity.QBaseTimeEntity(this);

    public final NumberPath<Integer> amount = createNumber("amount", Integer.class);

    public final NumberPath<Long> balanceId = createNumber("balanceId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> totalAmount = createNumber("totalAmount", Integer.class);

    public final EnumPath<kr.hhplus.be.server.domain.balance.type.TransactionType> type = createEnum("type", kr.hhplus.be.server.domain.balance.type.TransactionType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QBalanceHistory(String variable) {
        super(BalanceHistory.class, forVariable(variable));
    }

    public QBalanceHistory(Path<? extends BalanceHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBalanceHistory(PathMetadata metadata) {
        super(BalanceHistory.class, metadata);
    }

}

