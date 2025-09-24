-- ----------------------------
-- Table structure for t_chat
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_chat";
CREATE TABLE "public"."t_chat" (
                                   "id" int8 NOT NULL DEFAULT nextval('t_chat_id_seq'::regclass),
                                   "uuid" varchar(60) COLLATE "pg_catalog"."default" NOT NULL,
                                   "summary" varchar(60) COLLATE "pg_catalog"."default",
                                   "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   "role_id" int8
)
;
COMMENT ON COLUMN "public"."t_chat"."id" IS '主键ID，自增唯一标识';
COMMENT ON COLUMN "public"."t_chat"."uuid" IS '对话UUID，全局唯一标识';
COMMENT ON COLUMN "public"."t_chat"."summary" IS '对话摘要（最大长度60字符）';
COMMENT ON COLUMN "public"."t_chat"."create_time" IS '记录创建时间（默认当前时间）';
COMMENT ON COLUMN "public"."t_chat"."update_time" IS '记录最后更新时间（默认当前时间）';
COMMENT ON TABLE "public"."t_chat" IS '聊天对话表';

-- ----------------------------
-- Indexes structure for table t_chat
-- ----------------------------
CREATE INDEX "idx_chat_role_id" ON "public"."t_chat" USING btree (
    "role_id" "pg_catalog"."int8_ops" ASC NULLS LAST
    );
CREATE INDEX "idx_chat_update_time" ON "public"."t_chat" USING btree (
    "update_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Uniques structure for table t_chat
-- ----------------------------
ALTER TABLE "public"."t_chat" ADD CONSTRAINT "t_chat_uuid_key" UNIQUE ("uuid");

-- ----------------------------
-- Primary Key structure for table t_chat
-- ----------------------------
ALTER TABLE "public"."t_chat" ADD CONSTRAINT "t_chat_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Table structure for t_chat_message
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_chat_message";
CREATE TABLE "public"."t_chat_message" (
                                           "id" int8 NOT NULL DEFAULT nextval('t_chat_message_id_seq'::regclass),
                                           "chat_uuid" varchar(60) COLLATE "pg_catalog"."default" NOT NULL,
                                           "content" text COLLATE "pg_catalog"."default" NOT NULL,
                                           "role" varchar(12) COLLATE "pg_catalog"."default",
                                           "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."t_chat_message"."id" IS '主键ID，自增唯一标识';
COMMENT ON COLUMN "public"."t_chat_message"."chat_uuid" IS '关联的对话UUID，与t_chat表的uuid字段关联';
COMMENT ON COLUMN "public"."t_chat_message"."content" IS '消息内容';
COMMENT ON COLUMN "public"."t_chat_message"."role" IS '消息发送者角色（如：user-用户，assistant-助手）';
COMMENT ON COLUMN "public"."t_chat_message"."create_time" IS '消息创建时间（默认当前时间）';
COMMENT ON TABLE "public"."t_chat_message" IS '聊天消息记录表';

-- ----------------------------
-- Indexes structure for table t_chat_message
-- ----------------------------
CREATE INDEX "idx_chat_message_create_time" ON "public"."t_chat_message" USING btree (
    "create_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
    );
CREATE INDEX "idx_chat_message_uuid" ON "public"."t_chat_message" USING btree (
    "chat_uuid" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table t_chat_message
-- ----------------------------
ALTER TABLE "public"."t_chat_message" ADD CONSTRAINT "t_chat_message_pkey" PRIMARY KEY ("id");


-- ----------------------------
-- Table structure for t_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."t_role";
CREATE TABLE "public"."t_role" (
                                   "id" int8 NOT NULL DEFAULT nextval('t_role_id_seq'::regclass),
                                   "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
                                   "introduction" varchar(255) COLLATE "pg_catalog"."default",
                                   "system_prompt" text COLLATE "pg_catalog"."default" NOT NULL,
                                   "voice_code" varchar(100) COLLATE "pg_catalog"."default",
                                   "language" varchar(16) COLLATE "pg_catalog"."default" DEFAULT 'zh-CN'::character varying,
                                   "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   "voice_model_name" varchar(60) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."t_role"."system_prompt" IS '角色人设的系统提示';
COMMENT ON TABLE "public"."t_role" IS 'AI角色表';

-- ----------------------------
-- Indexes structure for table t_role
-- ----------------------------
CREATE INDEX "idx_role_name" ON "public"."t_role" USING btree (
    "name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table t_role
-- ----------------------------
ALTER TABLE "public"."t_role" ADD CONSTRAINT "t_role_pkey" PRIMARY KEY ("id");
