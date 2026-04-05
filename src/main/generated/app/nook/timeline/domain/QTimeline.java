package app.nook.timeline.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTimeline is a Querydsl query type for Timeline
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTimeline extends EntityPathBase<Timeline> {

    private static final long serialVersionUID = -1904911102L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTimeline timeline = new QTimeline("timeline");

    public final app.nook.global.common.QBaseEntity _super = new app.nook.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final app.nook.library.domain.QLibrary library;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final DateTimePath<java.time.LocalDateTime> occurredAt = createDateTime("occurredAt", java.time.LocalDateTime.class);

    public final StringPath previewText = createString("previewText");

    public final NumberPath<Long> targetId = createNumber("targetId", Long.class);

    public final EnumPath<app.nook.timeline.domain.enums.TimelineType> type = createEnum("type", app.nook.timeline.domain.enums.TimelineType.class);

    public QTimeline(String variable) {
        this(Timeline.class, forVariable(variable), INITS);
    }

    public QTimeline(Path<? extends Timeline> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTimeline(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTimeline(PathMetadata metadata, PathInits inits) {
        this(Timeline.class, metadata, inits);
    }

    public QTimeline(Class<? extends Timeline> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.library = inits.isInitialized("library") ? new app.nook.library.domain.QLibrary(forProperty("library"), inits.get("library")) : null;
    }

}

