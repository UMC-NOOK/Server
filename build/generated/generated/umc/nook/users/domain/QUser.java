package umc.nook.users.domain;

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

    private static final long serialVersionUID = -1434043359L;

    public static final QUser user = new QUser("user");

    public final umc.nook.QBaseTimeEntity _super = new umc.nook.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final StringPath email = createString("email");

    public final ListPath<umc.nook.readingrooms.domain.ReadingRoomUser, umc.nook.readingrooms.domain.QReadingRoomUser> joinedRooms = this.<umc.nook.readingrooms.domain.ReadingRoomUser, umc.nook.readingrooms.domain.QReadingRoomUser>createList("joinedRooms", umc.nook.readingrooms.domain.ReadingRoomUser.class, umc.nook.readingrooms.domain.QReadingRoomUser.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final StringPath nickname = createString("nickname");

    public final StringPath password = createString("password");

    public final EnumPath<RoleType> role = createEnum("role", RoleType.class);

    public final EnumPath<Status> status = createEnum("status", Status.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QUser(String variable) {
        super(User.class, forVariable(variable));
    }

    public QUser(Path<? extends User> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUser(PathMetadata metadata) {
        super(User.class, metadata);
    }

}

