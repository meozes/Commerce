package kr.hhplus.be.server.domain.balance.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBalance is a Querydsl query type for Balance
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBalance extends EntityPathBase<Balance> {

    private static final long serialVersionUID = 791136992L;

    public static final QBalance balance1 = new QBalance("balance1");

    public final kr.hhplus.be.server.domain.common.entity.QBaseTimeEntity _super = new kr.hhplus.be.server.domain.common.entity.QBaseTimeEntity(this);

    public final NumberPath<Integer> balance = createNumber("balance", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QBalance(String variable) {
        super(Balance.class, forVariable(variable));
    }

    public QBalance(Path<? extends Balance> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBalance(PathMetadata metadata) {
        super(Balance.class, metadata);
    }

}

