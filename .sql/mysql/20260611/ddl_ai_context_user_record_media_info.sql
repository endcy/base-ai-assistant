-- 为 ai_context_user_record 表新增 mediaInfo 字段，用于存储多媒体附件信息（JSON格式）
-- 执行时间：2026-06-13
-- 说明：支持多模态输入（图片、音频、视频等），mediaInfo存储MediaAttachment列表的JSON序列化数据

ALTER TABLE ai_context_user_record
    ADD COLUMN media_info TEXT DEFAULT NULL COMMENT '多媒体信息JSON（存储图片、音频、视频等多模态附件信息）' after question;

-- 示例JSON格式：
-- [
--   {"type":"IMAGE","url":"https://oss.example.com/img/xxx.png","description":"用户上传的设备截图","mimeType":"image/png"},
--   {"type":"AUDIO","url":"https://oss.example.com/audio/xxx.mp3","description":"用户语音留言","mimeType":"audio/mpeg"}
-- ]
