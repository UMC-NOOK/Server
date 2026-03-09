package app.nook.focus.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QFocus is a Querydsl query type for Focus
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFocus extends EntityPathBase<Focus> {

    private static final long serialVersionUID = 1783721416L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QFocus focus = new QFocus("focus");

    public final app.nook.global.common.QBaseEntity _super = new app.nook.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Integer> durationSec = createNumber("durationSec", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> endedAt = createDateTime("endedAt", java.time.LocalDateTime.class);

    public final TimePath<java.time.LocalTime> endedTime = createTime("endedTime", java.time.LocalTime.class);

    public final DatePath<java.time.LocalDate> focusDate = createDate("focusDate", java.time.LocalDate.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final app.nook.library.domain.QLibrary library;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final DateTimePath<java.time.LocalDateTime> startedAt = createDateTime("startedAt", java.time.LocalDateTime.class);

    public final TimePath<java.time.LocalTime> startedTime = createTime("startedTime", java.time.LocalTime.class);

    public final QTheme theme;

    public QFocus(String variable) {
        this(Focus.class, forVariable(variable), INITS);
    }

    public QFocus(Path<? extends Focus> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QFocus(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QFocus(PathMetadata metadata, PathInits inits) {
        this(Focus.class, metadata, inits);
    }

    public QFocus(Class<? extends Focus> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.library = inits.isInitialized("library") ? new app.nook.library.domain.QLibrary(forProperty("library"), inits.get("library")) : null;
        this.theme = inits.isInitialized("theme") ? new QTheme(forProperty("theme")) : null;
    }

}

