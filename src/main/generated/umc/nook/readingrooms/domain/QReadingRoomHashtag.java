package umc.nook.readingrooms.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReadingRoomHashtag is a Querydsl query type for ReadingRoomHashtag
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReadingRoomHashtag extends EntityPathBase<ReadingRoomHashtag> {

    private static final long serialVersionUID = 1997327707L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReadingRoomHashtag readingRoomHashtag = new QReadingRoomHashtag("readingRoomHashtag");

    public final umc.nook.QBaseTimeEntity _super = new umc.nook.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final QHashtag hashtag;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final QReadingRoom readingRoom;

    public QReadingRoomHashtag(String variable) {
        this(ReadingRoomHashtag.class, forVariable(variable), INITS);
    }

    public QReadingRoomHashtag(Path<? extends ReadingRoomHashtag> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReadingRoomHashtag(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReadingRoomHashtag(PathMetadata metadata, PathInits inits) {
        this(ReadingRoomHashtag.class, metadata, inits);
    }

    public QReadingRoomHashtag(Class<? extends ReadingRoomHashtag> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.hashtag = inits.isInitialized("hashtag") ? new QHashtag(forProperty("hashtag")) : null;
        this.readingRoom = inits.isInitialized("readingRoom") ? new QReadingRoom(forProperty("readingRoom"), inits.get("readingRoom")) : null;
    }

}

