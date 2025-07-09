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

    public static final QTheme theme = new QTheme("theme");

    public final umc.nook.QBaseTimeEntity _super = new umc.nook.QBaseTimeEntity(this);

    public final StringPath bgmUrl = createString("bgmUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final ListPath<ReadingRoom, QReadingRoom> readingRooms = this.<ReadingRoom, QReadingRoom>createList("readingRooms", ReadingRoom.class, QReadingRoom.class, PathInits.DIRECT2);

    public QTheme(String variable) {
        super(Theme.class, forVariable(variable));
    }

    public QTheme(Path<? extends Theme> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTheme(PathMetadata metadata) {
        super(Theme.class, metadata);
    }

}

