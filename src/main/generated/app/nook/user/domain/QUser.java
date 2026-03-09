package app.nook.user.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = -747444330L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUser user = new QUser("user");

    public final app.nook.global.common.QBaseEntity _super = new app.nook.global.common.QBaseEntity(this);

    public final NumberPath<Short> chatUsage = createNumber("chatUsage", Short.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final NumberPath<Short> goal = createNumber("goal", Short.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final StringPath nickName = createString("nickName");

    public final DateTimePath<java.time.LocalDateTime> onboardingCompletedAt = createDateTime("onboardingCompletedAt", java.time.LocalDateTime.class);

    public final app.nook.book.domain.QCategory preferredCategory;

    public final StringPath profileUrl = createString("profileUrl");

    public final StringPath provider = createString("provider");

    public final StringPath providerId = createString("providerId");

    public final EnumPath<app.nook.user.domain.enums.UserRole> role = createEnum("role", app.nook.user.domain.enums.UserRole.class);

    public final ListPath<app.nook.book.domain.SearchHistory, app.nook.book.domain.QSearchHistory> searchHistories = this.<app.nook.book.domain.SearchHistory, app.nook.book.domain.QSearchHistory>createList("searchHistories", app.nook.book.domain.SearchHistory.class, app.nook.book.domain.QSearchHistory.class, PathInits.DIRECT2);

    public final EnumPath<app.nook.user.domain.enums.UserStatus> status = createEnum("status", app.nook.user.domain.enums.UserStatus.class);

    public QUser(String variable) {
        this(User.class, forVariable(variable), INITS);
    }

    public QUser(Path<? extends User> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUser(PathMetadata metadata, PathInits inits) {
        this(User.class, metadata, inits);
    }

    public QUser(Class<? extends User> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.preferredCategory = inits.isInitialized("preferredCategory") ? new app.nook.book.domain.QCategory(forProperty("preferredCategory")) : null;
    }

}

