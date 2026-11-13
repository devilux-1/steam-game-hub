package com.steamhub.entity.DTO;

import lombok.Data;

/**
 * AI 翻译结果对象：模型按约定返回的 JSON 数组元素，id 对应 nexus_mod_cache 主键。
 */
@Data
public class ModTranslationDTO {

    /** 缓存表主键，用于把译文回填到对应记录 */
    private Long id;

    /** 中文标题，模型未给出时为空 */
    private String nameCn;

    /** 中文简介，模型未给出时为空 */
    private String summaryCn;
}
