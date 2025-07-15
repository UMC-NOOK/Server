package umc.nook.search.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRecentQuery is a Querydsl query type for RecentQuery
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRecentQuery extends EntityPathBase<RecentQuery> {

    private static final long serialVersionUID = -543826661L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRecentQuery recentQuery = new QRecentQuery("recentQuery");

    public final DateTimePath<java.time.LocalDateTime> createdDate = createDateTime("createdDate", java.time.LocalDateTime.class);

    public final StringPath query = createString("query");

    public final NumberPath<Long> recentQueryId = createNumber("recentQueryId", Long.class);

    public final umc.nook.users.domain.QUser user;

    public QRecentQuery(String variable) {
        this(RecentQuery.class, forVariable(variable), INITS);
    }

    public QRecentQuery(Path<? extends RecentQuery> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRecentQuery(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRecentQuery(PathMetadata metadata, PathInits inits) {
        this(RecentQuery.class, metadata, inits);
    }

    public QRecentQuery(Class<? extends RecentQuery> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new umc.nook.users.domain.QUser(forProperty("user")) : null;
    }

}

