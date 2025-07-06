package umc.nook.readingrooms.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReadingRoom is a Querydsl query type for ReadingRoom
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReadingRoom extends EntityPathBase<ReadingRoom> {

    private static final long serialVersionUID = 880966961L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReadingRoom readingRoom = new QReadingRoom("readingRoom");

    public final umc.nook.QBaseTimeEntity _super = new umc.nook.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final StringPath description = createString("description");

    public final ListPath<ReadingRoomHashtag, QReadingRoomHashtag> hashtags = this.<ReadingRoomHashtag, QReadingRoomHashtag>createList("hashtags", ReadingRoomHashtag.class, QReadingRoomHashtag.class, PathInits.DIRECT2);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final StringPath name = createString("name");

    public final QTheme theme;

    public final ListPath<ReadingRoomUser, QReadingRoomUser> users = this.<ReadingRoomUser, QReadingRoomUser>createList("users", ReadingRoomUser.class, QReadingRoomUser.class, PathInits.DIRECT2);

    public QReadingRoom(String variable) {
        this(ReadingRoom.class, forVariable(variable), INITS);
    }

    public QReadingRoom(Path<? extends ReadingRoom> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReadingRoom(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReadingRoom(PathMetadata metadata, PathInits inits) {
        this(ReadingRoom.class, metadata, inits);
    }

    public QReadingRoom(Class<? extends ReadingRoom> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.theme = inits.isInitialized("theme") ? new QTheme(forProperty("theme"), inits.get("theme")) : null;
    }

}

