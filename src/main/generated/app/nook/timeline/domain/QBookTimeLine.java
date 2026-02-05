package app.nook.timeline.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBookTimeLine is a Querydsl query type for BookTimeLine
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBookTimeLine extends EntityPathBase<BookTimeLine> {

    private static final long serialVersionUID = 16395947L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBookTimeLine bookTimeLine = new QBookTimeLine("bookTimeLine");

    public final app.nook.global.common.QBaseEntity _super = new app.nook.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final app.nook.library.domain.QLibrary library;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final StringPath snapshotValue = createString("snapshotValue");

    public final NumberPath<Long> targetId = createNumber("targetId", Long.class);

    public final EnumPath<app.nook.timeline.domain.enums.BookTimeLineType> type = createEnum("type", app.nook.timeline.domain.enums.BookTimeLineType.class);

    public QBookTimeLine(String variable) {
        this(BookTimeLine.class, forVariable(variable), INITS);
    }

    public QBookTimeLine(Path<? extends BookTimeLine> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBookTimeLine(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBookTimeLine(PathMetadata metadata, PathInits inits) {
        this(BookTimeLine.class, metadata, inits);
    }

    public QBookTimeLine(Class<? extends BookTimeLine> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.library = inits.isInitialized("library") ? new app.nook.library.domain.QLibrary(forProperty("library"), inits.get("library")) : null;
    }

}

