create table lmp."user"
(
    id         bigserial
        primary key,
    username   varchar(30)                                        not null
        constraint uk_user_username
            unique,
    phone      varchar(20)
        constraint uk_user_phone
            unique,
    password   varchar(30)                                        not null,
    token      varchar(1000),
    is_deleted smallint                 default 0                 not null,
    updated_at timestamp with time zone default CURRENT_TIMESTAMP not null
);

comment on table lmp."user" is '用户主表（账号密码登录）';

comment on column lmp."user".id is '用户唯一ID（主键）';

comment on column lmp."user".username is '登录账号（唯一，用于账号密码登录）';

comment on constraint uk_user_username on lmp."user" is '登录账号唯一（NULL不冲突，支持仅手机号注册）';

comment on column lmp."user".phone is '手机号（唯一，用于验证码/账号密码登录）';

comment on constraint uk_user_phone on lmp."user" is '手机号唯一（必填，确保验证码登录唯一标识）';

comment on column lmp."user".password is '加密密码（仅用于账号密码登录）';

comment on column lmp."user".token is '用户登录令牌';

comment on column lmp."user".is_deleted is '软删除：0-正常，1-已删除';

comment on column lmp."user".updated_at is '最后修改时间';

alter table lmp."user"
    owner to postgres;

create table lmp.session
(
    id            bigserial
        primary key,
    user_id       bigint                                             not null
        constraint fk_session_user_id
            references lmp."user"
            on update cascade on delete restrict,
    model_id      bigint                                             not null,
    session_title varchar(50)                                        not null,
    is_pinned     smallint                 default 0                 not null,
    is_collected  smallint                 default 0                 not null,
    created_at    timestamp with time zone default CURRENT_TIMESTAMP not null,
    updated_at    timestamp with time zone default CURRENT_TIMESTAMP not null,
    last_msg_time timestamp with time zone default CURRENT_TIMESTAMP not null,
    is_deleted    smallint                 default 0                 not null
);

comment on table lmp.session is '聊天会话表（管理用户的多个独立会话）';

comment on column lmp.session.id is '会话ID（主键）';

comment on column lmp.session.user_id is '关联用户ID（外键：lmp.user.id）';

comment on column lmp.session.model_id is '关联AI模型ID（外键：model.id）';

comment on column lmp.session.session_title is '会话标题（默认取首条消息部分内容）';

comment on column lmp.session.is_pinned is '是否置顶，是(1)，否(0)，默认0';

comment on column lmp.session.is_collected is '是否收藏，1：收藏，0：未收藏';

comment on column lmp.session.created_at is '会话创建时间';

comment on column lmp.session.updated_at is '会话更新时间';

comment on column lmp.session.last_msg_time is '最后一条消息的时间（用于会话列表排序）';

comment on column lmp.session.is_deleted is '软删除：0-正常，1-已删除';

alter table lmp.session
    owner to postgres;

create table lmp.folder
(
    id             bigserial
        primary key,
    user_id        bigint                                             not null
        constraint fk_folder_user_id
            references lmp."user"
            on update cascade on delete restrict,
    session_id     bigint
        constraint fk_folder_session_id
            references lmp.session
            on update cascade on delete cascade,
    anonymous_id   varchar(255),
    name           varchar(100)                                       not null,
    parent_id      bigint
        constraint fk_folder_parent_id
            references lmp.folder
            on update cascade on delete set null,
    created_at     timestamp                default CURRENT_TIMESTAMP not null,
    upload_id      varchar(64),
    upload_status  smallint                 default 0                 not null,
    total_files    integer                  default 0                 not null,
    uploaded_files integer                  default 0                 not null,
    is_deleted     smallint                 default 0                 not null,
    updated_at     timestamp with time zone default CURRENT_TIMESTAMP not null
);

comment on table lmp.folder is '用户上传的文件夹表（支持层级）';

comment on column lmp.folder.id is '文件夹ID';

comment on column lmp.folder.user_id is '所属用户ID（外键：user.id）';

comment on column lmp.folder.session_id is '关联会话ID（可为NULL：用户级公共文件夹）';

comment on column lmp.folder.anonymous_id is '未登录用户临时ID';

comment on column lmp.folder.name is '文件夹名称';

comment on column lmp.folder.parent_id is '父文件夹ID（NULL表示根目录，实现层级）';

comment on column lmp.folder.created_at is '创建时间';

comment on column lmp.folder.upload_id is '文件夹上传唯一标识（UUID，区别于chunk的数据）';

comment on column lmp.folder.upload_status is '文件夹上传状态：0-上传中，1-完成，2-失败';

comment on column lmp.folder.total_files is '文件夹内待上传的总文件数';

comment on column lmp.folder.uploaded_files is '已上传完成的文件数（用于进度展示）';

comment on column lmp.folder.is_deleted is '软删除：0-正常，1-已删除';

comment on column lmp.folder.updated_at is '最后修改时间';

alter table lmp.folder
    owner to postgres;

create index idx_folder_parent_id
    on lmp.folder (parent_id);

comment on index lmp.idx_folder_parent_id is '查询子文件夹（层级遍历）';

create index idx_folder_user_session
    on lmp.folder (user_id, session_id);

comment on index lmp.idx_folder_user_session is '查询用户在某会话下的文件夹';

create index idx_folder_upload_id
    on lmp.folder (upload_id, user_id);

comment on index lmp.idx_folder_upload_id is '查询用户的文件夹上传进度';

create index idx_folder_user_id
    on lmp.folder (user_id);

create table lmp.collection
(
    id            bigserial
        primary key,
    user_id       bigint                                             not null
        constraint fk_collection_user_id
            references lmp."user"
            on update cascade on delete restrict,
    session_id    bigint                                             not null
        constraint fk_collection_session_id
            references lmp.session
            on update cascade on delete cascade,
    session_title varchar(100),
    created_at    timestamp                default CURRENT_TIMESTAMP not null,
    is_deleted    smallint                 default 0                 not null,
    updated_at    timestamp with time zone default CURRENT_TIMESTAMP not null,
    constraint uk_collection_user_id_session_id
        unique (user_id, session_id)
);

comment on table lmp.collection is '用户会话收藏表（收藏整个聊天会话）';

comment on column lmp.collection.id is '收藏ID（主键）';

comment on column lmp.collection.user_id is '关联用户ID（外键：user.id）';

comment on column lmp.collection.session_id is '关联会话ID（外键：session.id）';

comment on column lmp.collection.session_title is '收藏的对话标题';

comment on column lmp.collection.created_at is '收藏创建时间';

comment on column lmp.collection.is_deleted is '软删除：0-正常，1-已删除';

comment on column lmp.collection.updated_at is '最后修改时间';

comment on constraint uk_collection_user_id_session_id on lmp.collection is '同一用户不能重复收藏同一个会话';

alter table lmp.collection
    owner to postgres;

create index idx_collection_user_id_created_at
    on lmp.collection (user_id, created_at);

comment on index lmp.idx_collection_user_id_created_at is '按时间查询用户的收藏';

create index idx_collection_session_id
    on lmp.collection (session_id);

create index idx_collection_user_session
    on lmp.collection (user_id, session_id);

create table lmp.message
(
    id                bigserial
        primary key,
    user_id           bigint                                             not null
        constraint fk_message_user_id
            references lmp."user"
            on update cascade on delete restrict,
    session_id        bigint                                             not null
        constraint fk_message_session_id
            references lmp.session
            on update cascade on delete cascade,
    role              smallint                                           not null,
    thinking          text,
    content           text                                               not null,
    type              smallint                 default 1                 not null,
    file_ids          varchar(500),
    token_count       integer                  default 0                 not null,
    is_deep_think     smallint                 default 0                 not null,
    is_network_search smallint                 default 0                 not null,
    sent_at           timestamp                default CURRENT_TIMESTAMP not null,
    is_deleted        smallint                 default 0                 not null,
    updated_at        timestamp with time zone default CURRENT_TIMESTAMP not null
)
with (fillfactor = 90);

comment on table lmp.message is '聊天消息表（存储用户与AI的单条交互内容）';

comment on column lmp.message.id is '消息ID（主键）';

comment on column lmp.message.user_id is '关联用户ID（外键：user.id）';

comment on column lmp.message.session_id is '关联会话ID（外键：lmp.session.id）';

comment on column lmp.message.role is '消息类型：1-用户提问，2-AI回复';

comment on column lmp.message.thinking is '思考过程';

comment on column lmp.message.content is '消息内容（用户的文本提问/AI的文本回复，支持Markdown）';

comment on column lmp.message.type is '表示消息类型，1:text，2:file ，3:image';

comment on column lmp.message.file_ids is '对话消息关联的文件，用逗号分隔';

comment on column lmp.message.token_count is '消息消耗的Token数（AI计费/长度限制用）';

comment on column lmp.message.is_deep_think is '是否开启深度思考：1-是，0-否';

comment on column lmp.message.is_network_search is '是否开启联网搜索：1-是，0-否';

comment on column lmp.message.sent_at is '消息发送/生成时间';

comment on column lmp.message.is_deleted is '软删除：0-正常，1-已删除';

comment on column lmp.message.updated_at is '最后修改时间';

alter table lmp.message
    owner to postgres;

create index idx_message_session_id
    on lmp.message (session_id);

comment on index lmp.idx_message_session_id is '查询某会话下的所有消息';

create index idx_message_user_id
    on lmp.message (user_id);

create index idx_message_user_session
    on lmp.message (user_id, session_id);

create index idx_message_session_send_time
    on lmp.message (user_id asc, session_id asc, sent_at desc);

comment on index lmp.idx_message_session_send_time is '查询用户会话消息并按发送时间排序';

create index idx_session_user_id
    on lmp.session (user_id);

comment on index lmp.idx_session_user_id is '按用户查询所有会话';

create index idx_session_user_last_msg_time
    on lmp.session (user_id asc, last_msg_time desc);

comment on index lmp.idx_session_user_last_msg_time is '查询用户会话并按最后消息时间排序';

create table lmp.user_profile
(
    id              bigserial
        primary key,
    user_id         bigint                              not null
        constraint uk_user_profile_user_id
            unique
        constraint fk_user_profile_user_id
            references lmp."user"
            on update cascade on delete cascade,
    avatar          varchar(255),
    sex             smallint,
    email           varchar(30),
    birthday        timestamp,
    bio             varchar(100),
    last_login_time timestamp,
    last_login_ip   varchar(50),
    created_at      timestamp default CURRENT_TIMESTAMP not null,
    updated_at      timestamp default CURRENT_TIMESTAMP not null
);

comment on table lmp.user_profile is '用户资料/档案表（存储用户头像、性别、简介等非登录类扩展信息）';

comment on column lmp.user_profile.id is '用户信息唯一ID（主键）';

comment on column lmp.user_profile.user_id is '用户ID';

comment on column lmp.user_profile.avatar is '头像URL';

comment on column lmp.user_profile.sex is '性别，男(1)，女(0)';

comment on column lmp.user_profile.email is '邮箱';

comment on column lmp.user_profile.birthday is '生日';

comment on column lmp.user_profile.bio is '简介';

comment on column lmp.user_profile.last_login_time is '最后登录时间（无论哪种登录方式）';

comment on column lmp.user_profile.last_login_ip is '最后登录IP';

comment on column lmp.user_profile.created_at is '注册时间';

comment on column lmp.user_profile.updated_at is '信息更新时间';

alter table lmp.user_profile
    owner to postgres;

create table lmp.model
(
    id          bigserial
        primary key,
    name        varchar(50)                                        not null,
    description varchar(200),
    is_default  smallint                 default 0                 not null,
    status      smallint                 default 1                 not null,
    max_token   integer                                            not null,
    prompt      varchar(4096),
    updated_at  timestamp with time zone default CURRENT_TIMESTAMP not null
);

comment on table lmp.model is 'AI模型表（管理可用的AI模型配置）';

comment on column lmp.model.id is '模型ID（主键）';

comment on column lmp.model.name is '模型名称';

comment on column lmp.model.description is '模型描述';

comment on column lmp.model.is_default is '是否默认模型：1-是（新会话默认使用），0-否';

comment on column lmp.model.status is '模型状态：1-启用，0-停用（维护时禁用）';

comment on column lmp.model.max_token is '模型最大Token限制（单次对话长度）';

comment on column lmp.model.prompt is '模型默认提示词';

comment on column lmp.model.updated_at is '最后修改时间';

alter table lmp.model
    owner to postgres;

create index idx_model_status_default
    on lmp.model (status, is_default);

comment on index lmp.idx_model_status_default is '查询启用的默认模型';

create table lmp.file
(
    id            bigserial
        primary key,
    user_id       bigint                                                      not null
        constraint fk_file_user_id
            references lmp."user"
            on update cascade on delete restrict,
    session_id    bigint
        constraint fk_file_session_id
            references lmp.session
            on update cascade on delete cascade,
    anonymous_id  varchar(255),
    folder_id     bigint
        constraint fk_file_folder_id
            references lmp.folder
            on update cascade on delete cascade,
    new_name      varchar(512)                                                not null,
    original_name varchar(512),
    extension     varchar(20),
    size          bigint                                                      not null,
    storage_path  varchar(1024)                                               not null,
    access_url    varchar(1024),
    is_image      smallint                 default 0                          not null,
    image_width   integer,
    image_height  integer,
    upload_at     timestamp with time zone default CURRENT_TIMESTAMP          not null,
    file_md5      varchar(64)                                                 not null,
    storage_type  varchar(20)              default 'local'::character varying not null,
    bucket_name   varchar(100),
    expire_at     timestamp with time zone,
    mime_type     varchar(100),
    is_compressed smallint                 default 0                          not null,
    relative_path varchar(500),
    is_deleted    smallint                 default 0                          not null,
    updated_at    timestamp with time zone default CURRENT_TIMESTAMP          not null,
    constraint uk_file_user_folder_new_name
        unique (user_id, folder_id, new_name)
);

comment on table lmp.file is '用户上传的文件/图片主表（最终落地的文件信息）';

comment on column lmp.file.id is '文件ID';

comment on column lmp.file.user_id is '上传用户ID（外键：user.id）';

comment on column lmp.file.session_id is '关联会话ID（外键：chat_session.id）';

comment on column lmp.file.anonymous_id is '未登录用户临时ID';

comment on column lmp.file.folder_id is '所属文件夹ID（关联lmp.folder）';

comment on column lmp.file.new_name is '新文件名（系统生成，唯一）';

comment on column lmp.file.original_name is '原始文件名（用户上传的名称）';

comment on column lmp.file.extension is '文件扩展名（如jpg、pdf）';

comment on column lmp.file.size is '文件大小（字节）';

comment on column lmp.file.storage_path is '文件存储路径（本地路径/OSS对象键）';

comment on column lmp.file.access_url is '文件访问URL（前端可直接访问）';

comment on column lmp.file.is_image is '是否图片：1-是，0-否';

comment on column lmp.file.image_width is '图片宽度（像素，仅is_image=1时有值）';

comment on column lmp.file.image_height is '图片高度（像素，仅is_image=1时有值）';

comment on column lmp.file.upload_at is '文件最终上传完成时间';

comment on column lmp.file.file_md5 is '文件MD5校验码（去重+完整性校验）';

comment on column lmp.file.storage_type is '存储类型：local-本地，oss-阿里云OSS，minio-MinIO';

comment on column lmp.file.bucket_name is '存储桶名称（OSS/MinIO场景使用）';

comment on column lmp.file.expire_at is '文件过期时间（NULL表示永久有效）';

comment on column lmp.file.mime_type is '文件MIME类型（如image/jpeg）';

comment on column lmp.file.is_compressed is '是否压缩文件：1-是，0-否';

comment on column lmp.file.relative_path is '文件相对路径（还原文件夹目录结构）';

comment on column lmp.file.is_deleted is '软删除：0-正常，1-已删除';

comment on column lmp.file.updated_at is '最后修改时间';

comment on constraint uk_file_user_folder_new_name on lmp.file is '同一用户同一文件夹下新文件名唯一';

alter table lmp.file
    owner to postgres;

create table lmp.file_content
(
    id         bigserial
        primary key,
    file_id    bigint                                             not null
        constraint fk_file_content_file_id
            references lmp.file
            on update cascade on delete cascade,
    type       varchar(32)                                        not null,
    content    text                                               not null,
    created_at timestamp                default CURRENT_TIMESTAMP,
    is_deleted smallint                 default 0                 not null,
    updated_at timestamp with time zone default CURRENT_TIMESTAMP not null
);

comment on column lmp.file_content.file_id is '关联文件ID';

comment on column lmp.file_content.type is '解析内容类型';

comment on column lmp.file_content.content is '解析之后的内容';

comment on column lmp.file_content.created_at is '创建时间';

comment on column lmp.file_content.is_deleted is '软删除：0-正常，1-已删除';

comment on column lmp.file_content.updated_at is '最后修改时间';

alter table lmp.file_content
    owner to postgres;

create index idx_file_content_file_id
    on lmp.file_content (file_id);

create index idx_file_content_type
    on lmp.file_content (type);

create index idx_file_content_file_type
    on lmp.file_content (file_id, type);

create index idx_file_user_id
    on lmp.file (user_id);

create index idx_file_user_session
    on lmp.file (user_id, session_id);

create index idx_file_md5
    on lmp.file (file_md5);

create index idx_file_storage_type
    on lmp.file (storage_type);

create index idx_file_folder_id
    on lmp.file (folder_id, user_id);

create index idx_file_anonym_md5
    on lmp.file (anonymous_id, file_md5);

create index idx_file_expire_at
    on lmp.file (expire_at);

create index idx_file_folder_upload_at
    on lmp.file (folder_id asc, upload_at desc);

comment on index lmp.idx_file_folder_upload_at is '查询文件夹内文件并按上传时间排序';

create unique index idx_file_user_id_md5
    on lmp.file (user_id, file_md5)
    where (user_id IS NOT NULL);

comment on index lmp.idx_file_user_id_md5 is '登录用户：user_id+file_md5唯一，避免重复上传';

create unique index idx_file_anonymous_id_md5
    on lmp.file (anonymous_id, file_md5)
    where (anonymous_id IS NOT NULL);

comment on index lmp.idx_file_anonymous_id_md5 is '匿名用户：anonymous_id+file_md5唯一，避免重复上传';

create unique index idx_file_user_session_md5
    on lmp.file (user_id, session_id, file_md5);

create table lmp.file_chunk
(
    id              bigserial
        primary key,
    upload_id       varchar(64)                                    not null,
    file_id         bigint
        constraint fk_file_chunk_file_id
            references lmp.file
            on update cascade on delete cascade,
    user_id         bigint                                         not null
        constraint fk_file_chunk_user_id
            references lmp."user"
            on update cascade on delete restrict,
    folder_id       bigint
        constraint fk_file_chunk_folder_id
            references lmp.folder
            on update cascade on delete set null,
    file_md5        varchar(64)                                    not null,
    original_name   varchar(512)                                   not null,
    total_chunks    integer                                        not null,
    uploaded_chunks integer     default 0                          not null,
    upload_status   smallint    default 0                          not null,
    storage_type    varchar(20) default 'local'::character varying not null,
    bucket_name     varchar(100),
    relative_path   varchar(500),
    created_at      timestamp   default CURRENT_TIMESTAMP          not null,
    updated_at      timestamp   default CURRENT_TIMESTAMP          not null,
    expire_at       timestamp,
    is_deleted      smallint    default 0                          not null,
    source_path     varchar(500),
    target_path     varchar(500),
    constraint uk_chunk_upload_id_user
        unique (upload_id, user_id)
)
with (fillfactor = 90);

comment on table lmp.file_chunk is '分片上传进度表（仅存储分片上传的元信息和进度）';

comment on column lmp.file_chunk.id is '进度记录ID';

comment on column lmp.file_chunk.upload_id is '分片上传唯一标识（OSS/MinIO返回的uploadId或者local生成的UUID）';

comment on column lmp.file_chunk.file_id is '关联文件主表ID（外键：lmp.file.id）';

comment on column lmp.file_chunk.user_id is '上传用户ID（冗余，提升查询效率）';

comment on column lmp.file_chunk.folder_id is '所属文件夹ID';

comment on column lmp.file_chunk.file_md5 is '文件MD5（合并后用于匹配文件主表）';

comment on column lmp.file_chunk.original_name is '原始文件名（合并后同步到文件主表）';

comment on column lmp.file_chunk.total_chunks is '总分片数';

comment on column lmp.file_chunk.uploaded_chunks is '已上传分片数（断点续传进度）';

comment on column lmp.file_chunk.upload_status is '上传状态：0-上传中，1-上传完成（待合并），2-合并完成，3-上传失败，4-已过期';

comment on column lmp.file_chunk.storage_type is '存储类型：local-本地，oss-阿里云OSS，minio-MinIO';

comment on column lmp.file_chunk.bucket_name is '存储桶名称（OSS/MinIO场景）';

comment on column lmp.file_chunk.relative_path is '文件相对路径';

comment on column lmp.file_chunk.created_at is '分片上传初始化时间';

comment on column lmp.file_chunk.updated_at is '进度最后更新时间';

comment on column lmp.file_chunk.expire_at is '分片上传过期时间（超时未合并自动清理）';

comment on column lmp.file_chunk.is_deleted is '软删除：0-正常，1-已删除';

comment on column lmp.file_chunk.source_path is 'OSS临时/目标路径';

comment on column lmp.file_chunk.target_path is '最终存储路径';

comment on constraint uk_chunk_upload_id_user on lmp.file_chunk is '同一用户的同一uploadId唯一（避免重复上传）';

alter table lmp.file_chunk
    owner to postgres;

create index idx_file_chunk_upload_id_user_id
    on lmp.file_chunk (upload_id, user_id);

create index idx_chunk_upload_status
    on lmp.file_chunk (upload_status, user_id);

create index idx_chunk_upload_md5_user
    on lmp.file_chunk (file_md5, user_id);

create index idx_chunk_upload_expire_at
    on lmp.file_chunk (expire_at);

create index idx_chunk_upload_create_at
    on lmp.file_chunk (created_at);

create index idx_file_chunk_status_expire
    on lmp.file_chunk (upload_status, expire_at);

comment on index lmp.idx_file_chunk_status_expire is '清理过期/失败的分片上传记录';

create unique index uk_file_chunk_upload_user
    on lmp.file_chunk (upload_id, user_id)
    where (is_deleted = 0);

create table lmp.file_chunk_part
(
    id           bigserial
        primary key,
    upload_id    varchar(64)                                        not null,
    chunk_id     bigint                                             not null
        constraint fk_chunk_part_upload_id
            references lmp.file_chunk
            on delete cascade,
    part_number  integer                                            not null,
    e_tag        varchar(64)                                        not null,
    chunk_size   bigint                                             not null,
    storage_path varchar(1024),
    upload_at    timestamp                default CURRENT_TIMESTAMP not null,
    is_deleted   smallint                 default 0                 not null,
    updated_at   timestamp with time zone default CURRENT_TIMESTAMP not null,
    constraint uk_chunk_part_upload_id_number
        unique (upload_id, part_number)
)
with (fillfactor = 90);

comment on table lmp.file_chunk_part is '分片明细记录表（存储每个分片的ETag、分片号等信息）';

comment on column lmp.file_chunk_part.id is '分片明细ID';

comment on column lmp.file_chunk_part.upload_id is '关联分片进度表的uploadId';

comment on column lmp.file_chunk_part.chunk_id is '关联分片进度表ID（外键：lmp.file_chunk.id）';

comment on column lmp.file_chunk_part.part_number is '分片号（从1开始）';

comment on column lmp.file_chunk_part.e_tag is '分片ETag（OSS/MinIO返回，合并时必需）';

comment on column lmp.file_chunk_part.chunk_size is '该分片大小（字节）';

comment on column lmp.file_chunk_part.storage_path is '分片临时存储路径（本地路径/OSS对象键）';

comment on column lmp.file_chunk_part.upload_at is '该分片上传完成时间';

comment on column lmp.file_chunk_part.is_deleted is '软删除：0-正常，1-已删除';

comment on column lmp.file_chunk_part.updated_at is '最后修改时间';

comment on constraint uk_chunk_part_upload_id_number on lmp.file_chunk_part is '同一uploadId下分片号唯一（避免重复上传同一分片）';

alter table lmp.file_chunk_part
    owner to postgres;

create index idx_chunk_part_upload_id
    on lmp.file_chunk_part (upload_id);

create unique index uk_file_chunk_part_upload_part
    on lmp.file_chunk_part (upload_id, part_number)
    where (is_deleted = 0);

create index idx_file_chunk_part_upload
    on lmp.file_chunk_part (upload_id);

