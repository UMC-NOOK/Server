package umc.nook.readingrooms.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReadingRoomUser is a Querydsl query type for ReadingRoomUser
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReadingRoomUser extends EntityPathBase<ReadingRoomUser> {

    private static final long serialVersionUID = 131521692L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReadingRoomUser readingRoomUser = new QReadingRoomUser("readingRoomUser");

    public final umc.nook.QBaseTimeEntity _super = new umc.nook.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> lastAccessedAt = createDateTime("lastAccessedAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final QReadingRoom readingRoom;

    public final EnumPath<Role> role = createEnum("role", Role.class);

    public final umc.nook.users.domain.QUser user;

    public QReadingRoomUser(String variable) {
        this(ReadingRoomUser.class, forVariable(variable), INITS);
    }

    public QReadingRoomUser(Path<? extends ReadingRoomUser> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReadingRoomUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReadingRoomUser(PathMetadata metadata, PathInits inits) {
        this(ReadingRoomUser.class, metadata, inits);
    }

    public QReadingRoomUser(Class<? extends ReadingRoomUser> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.readingRoom = inits.isInitialized("readingRoom") ? new QReadingRoom(forProperty("readingRoom"), inits.get("readingRoom")) : null;
        this.user = inits.isInitialized("user") ? new umc.nook.users.domain.QUser(forProperty("user")) : null;
    }

}

