package umc.nook.readingrooms.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTheme is a Querydsl query type for Theme
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTheme extends EntityPathBase<Theme> {

    private static final long serialVersionUID = -1894851341L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTheme theme = new QTheme("theme");

    public final umc.nook.QBaseTimeEntity _super = new umc.nook.QBaseTimeEntity(this);

    public final StringPath bgmUrl = createString("bgmUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final QReadingRoom readingRoom;

    public QTheme(String variable) {
        this(Theme.class, forVariable(variable), INITS);
    }

    public QTheme(Path<? extends Theme> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTheme(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTheme(PathMetadata metadata, PathInits inits) {
        this(Theme.class, metadata, inits);
    }

    public QTheme(Class<? extends Theme> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.readingRoom = inits.isInitialized("readingRoom") ? new QReadingRoom(forProperty("readingRoom"), inits.get("readingRoom")) : null;
    }

}

