package com.ihdrs.backend.service;

import com.ihdrs.backend.entity.FeedbackData;
import com.ihdrs.backend.entity.RecognitionRecord;
import com.ihdrs.backend.repository.FeedbackDataRepository;
import com.ihdrs.backend.repository.ModelRepository;
import com.ihdrs.backend.repository.RecognitionRecordRepository;
import com.ihdrs.backend.repository.UserRepository;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.borders.SolidBorder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final RecognitionRecordRepository recordRepository;
    private final FeedbackDataRepository feedbackRepository;
    private final ModelRepository modelRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 识别历史导出 ====================

    /**
     * 导出识别历史数据
     */
    public void exportRecognitionHistory(HttpServletResponse response, String format, String scope,
                                         String fieldsStr, Integer page, Integer size,
                                         Integer result, Long userId,
                                         LocalDateTime startTime, LocalDateTime endTime) throws IOException {

        List<String> fields = parseFields(fieldsStr, getDefaultRecognitionFields());
        List<RecognitionRecord> records = getRecognitionData(scope, page, size, result, userId, startTime, endTime);
        String filename = "识别历史报表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        switch (format.toLowerCase()) {
            case "csv":
                exportRecognitionToCsv(response, records, fields, filename);
                break;
            case "pdf":
                exportRecognitionToPdf(response, records, fields, filename);
                break;
            case "excel":
            default:
                exportRecognitionToExcel(response, records, fields, filename);
                break;
        }
    }

    /**
     * 获取识别历史数据
     */
    private List<RecognitionRecord> getRecognitionData(String scope, Integer page, Integer size,
                                                       Integer result, Long userId,
                                                       LocalDateTime startTime, LocalDateTime endTime) {
        if ("current".equals(scope) && page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
            Page<RecognitionRecord> pageData = recordRepository.findAllWithFiltersAndUser(result, userId, startTime, endTime, pageable);
            return pageData.getContent();
        } else if ("all".equals(scope)) {
            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createTime"));
            Page<RecognitionRecord> pageData = recordRepository.findAllWithFiltersAndUser(null, null, null, null, pageable);
            return pageData.getContent();
        } else {
            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createTime"));
            Page<RecognitionRecord> pageData = recordRepository.findAllWithFiltersAndUser(result, userId, startTime, endTime, pageable);
            return pageData.getContent();
        }
    }

    /**
     * 导出识别历史到Excel
     */
    private void exportRecognitionToExcel(HttpServletResponse response, List<RecognitionRecord> records,
                                          List<String> fields, String filename) throws IOException {
        setExcelResponseHeaders(response, filename);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("识别历史");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            Row headerRow = sheet.createRow(0);
            Map<String, String> fieldNameMap = getRecognitionFieldNameMap();
            int colIndex = 0;
            for (String field : fields) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(fieldNameMap.getOrDefault(field, field));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (RecognitionRecord record : records) {
                Row row = sheet.createRow(rowIndex++);
                colIndex = 0;
                for (String field : fields) {
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(colIndex++);
                    cell.setCellValue(getRecognitionFieldValue(record, field));
                    cell.setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < fields.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }

    /**
     * 导出识别历史到CSV
     */
    private void exportRecognitionToCsv(HttpServletResponse response, List<RecognitionRecord> records,
                                        List<String> fields, String filename) throws IOException {
        setCsvResponseHeaders(response, filename);

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write('\ufeff');
            Map<String, String> fieldNameMap = getRecognitionFieldNameMap();

            List<String> headers = new ArrayList<>();
            for (String field : fields) {
                headers.add(fieldNameMap.getOrDefault(field, field));
            }
            writer.println(String.join(",", headers));

            for (RecognitionRecord record : records) {
                List<String> values = new ArrayList<>();
                for (String field : fields) {
                    String value = getRecognitionFieldValue(record, field);
                    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                        value = "\"" + value.replace("\"", "\"\"") + "\"";
                    }
                    values.add(value);
                }
                writer.println(String.join(",", values));
            }
        }
    }

    /**
     * 导出识别历史到PDF
     */
    private void exportRecognitionToPdf(HttpServletResponse response, List<RecognitionRecord> records,
                                        List<String> fields, String filename) throws IOException {
        setPdfResponseHeaders(response, filename);

        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(response.getOutputStream()));
        Document document = new Document(pdfDoc);

        try {
            // 获取中文字体
            PdfFont font = getChineseFont();

            // 添加标题
            Paragraph title = new Paragraph("识别历史报表")
                    .setFont(font)
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);
            document.add(title);

            // 添加生成时间
            Paragraph timeInfo = new Paragraph("生成时间：" + LocalDateTime.now().format(DATE_FORMATTER))
                    .setFont(font)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginBottom(15);
            document.add(timeInfo);

            // 创建表格
            float[] columnWidths = new float[fields.size()];
            Arrays.fill(columnWidths, 1f);
            Table table = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();

            Map<String, String> fieldNameMap = getRecognitionFieldNameMap();

            // 添加表头
            DeviceRgb headerBgColor = new DeviceRgb(66, 139, 202);
            for (String field : fields) {
                Cell headerCell = new Cell()
                        .add(new Paragraph(fieldNameMap.getOrDefault(field, field))
                                .setFont(font)
                                .setFontSize(9)
                                .setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(headerBgColor)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(5);
                table.addHeaderCell(headerCell);
            }

            // 添加数据行
            boolean isOddRow = false;
            DeviceRgb oddRowColor = new DeviceRgb(245, 245, 245);

            for (RecognitionRecord record : records) {
                for (String field : fields) {
                    String value = getRecognitionFieldValue(record, field);
                    Cell dataCell = new Cell()
                            .add(new Paragraph(value != null && !value.isEmpty() ? value : "-")
                                    .setFont(font)
                                    .setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER)
                            .setPadding(4);

                    if (isOddRow) {
                        dataCell.setBackgroundColor(oddRowColor);
                    }
                    table.addCell(dataCell);
                }
                isOddRow = !isOddRow;
            }

            document.add(table);

            // 添加统计信息
            Paragraph stats = new Paragraph("共 " + records.size() + " 条记录")
                    .setFont(font)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(15);
            document.add(stats);

        } catch (Exception e) {
            log.error("PDF导出失败", e);
            throw new IOException("PDF导出失败: " + e.getMessage(), e);
        } finally {
            document.close();
        }
    }

    /**
     * 获取识别记录字段值
     */
    private String getRecognitionFieldValue(RecognitionRecord record, String field) {
        try {
            switch (field) {
                case "recordId":
                    return record.getRecordId() != null ? record.getRecordId().toString() : "";
                case "userId":
                    return record.getUserId() != null ?  record.getUserId().toString() : "匿名";
                case "recognitionResult":
                    if (record.getRecognitionResult() != null) {
                        return record.getRecognitionResult().toString();
                    } else if (record.getSequenceResult() != null) {
                        return record.getSequenceResult();
                    }
                    return "";
                case "confidence":
                    return record.getConfidence() != null ?
                            String.format("%.2f%%", record.getConfidence().doubleValue() * 100) : "";
                case "modelName":
                    if (record.getModelId() != null) {
                        return modelRepository.findById(record.getModelId())
                                .map(m -> m.getModelName())
                                .orElse("");
                    }
                    return "";
                case "modelVersion":
                    if (record.getModelId() != null) {
                        return modelRepository.findById(record.getModelId())
                                .map(m -> m.getModelVersion())
                                .orElse("");
                    }
                    return "";
                case "inputType":
                    return record.getInputType() != null ? getInputTypeText(record.getInputType().name()) : "";
                case "processingTime":
                    return record.getProcessingTime() != null ? record.getProcessingTime() + "ms" : "";
                case "isCorrect":
                    if (record.getIsCorrect() == null) return "未确认";
                    return record.getIsCorrect() ? "正确" : "错误";
                case "createTime":
                    return record.getCreateTime() != null ? record.getCreateTime().format(DATE_FORMATTER) : "";
                default:
                    return "";
            }
        } catch (Exception e) {
            log.warn("获取字段值失败: field={}, error={}", field, e.getMessage());
            return "";
        }
    }

    /**
     * 获取识别历史字段名称映射
     */
    private Map<String, String> getRecognitionFieldNameMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("recordId", "记录ID");
        map.put("userId", "用户ID");
        map.put("recognitionResult", "识别结果");
        map.put("confidence", "置信度");
        map.put("modelName", "模型名称");
        map.put("modelVersion", "模型版本");
        map.put("inputType", "输入方式");
        map.put("processingTime", "处理时间");
        map.put("isCorrect", "正确性");
        map.put("createTime", "识别时间");
        return map;
    }

    /**
     * 获取默认识别历史导出字段
     */
    private List<String> getDefaultRecognitionFields() {
        return Arrays.asList("recordId", "userId", "recognitionResult", "confidence",
                "modelName", "modelVersion", "inputType", "processingTime", "isCorrect", "createTime");
    }

    // ==================== 反馈数据导出 ====================

    /**
     * 导出反馈数据
     */
    public void exportFeedback(HttpServletResponse response, String format, String scope,
                               String fieldsStr, Integer page, Integer size,
                               FeedbackData.FeedbackStatus status,
                               FeedbackData.FeedbackType feedbackType) throws IOException {

        List<String> fields = parseFields(fieldsStr, getDefaultFeedbackFields());
        List<FeedbackData> feedbackList = getFeedbackData(scope, page, size, status, feedbackType);
        String filename = "反馈数据报表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        switch (format.toLowerCase()) {
            case "csv":
                exportFeedbackToCsv(response, feedbackList, fields, filename);
                break;
            case "pdf":
                exportFeedbackToPdf(response, feedbackList, fields, filename);
                break;
            case "excel":
            default:
                exportFeedbackToExcel(response, feedbackList, fields, filename);
                break;
        }
    }

    /**
     * 获取反馈数据
     */
    private List<FeedbackData> getFeedbackData(String scope, Integer page, Integer size,
                                               FeedbackData.FeedbackStatus status,
                                               FeedbackData.FeedbackType feedbackType) {
        Pageable pageable;
        Page<FeedbackData> pageData;

        if ("current".equals(scope) && page != null && size != null) {
            pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        } else {
            pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createTime"));
        }

        if ("all".equals(scope)) {
            pageData = feedbackRepository.findAll(pageable);
        } else if (status != null && feedbackType != null) {
            pageData = feedbackRepository.findByFeedbackTypeAndStatusOrderByCreateTimeDesc(feedbackType, status, pageable);
        } else if (status != null) {
            pageData = feedbackRepository.findByStatusOrderByCreateTimeDesc(status, pageable);
        } else if (feedbackType != null) {
            pageData = feedbackRepository.findByFeedbackTypeOrderByCreateTimeDesc(feedbackType, pageable);
        } else {
            pageData = feedbackRepository.findAll(pageable);
        }

        return pageData.getContent();
    }

    /**
     * 导出反馈数据到Excel
     */
    private void exportFeedbackToExcel(HttpServletResponse response, List<FeedbackData> feedbackList,
                                       List<String> fields, String filename) throws IOException {
        setExcelResponseHeaders(response, filename);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("反馈数据");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            Row headerRow = sheet.createRow(0);
            Map<String, String> fieldNameMap = getFeedbackFieldNameMap();
            int colIndex = 0;
            for (String field : fields) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(fieldNameMap.getOrDefault(field, field));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (FeedbackData feedback : feedbackList) {
                Row row = sheet.createRow(rowIndex++);
                colIndex = 0;
                for (String field : fields) {
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(colIndex++);
                    cell.setCellValue(getFeedbackFieldValue(feedback, field));
                    cell.setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < fields.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }

    /**
     * 导出反馈数据到CSV
     */
    private void exportFeedbackToCsv(HttpServletResponse response, List<FeedbackData> feedbackList,
                                     List<String> fields, String filename) throws IOException {
        setCsvResponseHeaders(response, filename);

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write('\ufeff');
            Map<String, String> fieldNameMap = getFeedbackFieldNameMap();

            List<String> headers = new ArrayList<>();
            for (String field : fields) {
                headers.add(fieldNameMap.getOrDefault(field, field));
            }
            writer.println(String.join(",", headers));

            for (FeedbackData feedback : feedbackList) {
                List<String> values = new ArrayList<>();
                for (String field : fields) {
                    String value = getFeedbackFieldValue(feedback, field);
                    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                        value = "\"" + value.replace("\"", "\"\"") + "\"";
                    }
                    values.add(value);
                }
                writer.println(String.join(",", values));
            }
        }
    }

    /**
     * 导出反馈数据到PDF
     */
    private void exportFeedbackToPdf(HttpServletResponse response, List<FeedbackData> feedbackList,
                                     List<String> fields, String filename) throws IOException {
        setPdfResponseHeaders(response, filename);

        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(response.getOutputStream()));
        Document document = new Document(pdfDoc);

        try {
            // 获取中文字体
            PdfFont font = getChineseFont();

            // 添加标题
            Paragraph title = new Paragraph("反馈数据报表")
                    .setFont(font)
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);
            document.add(title);

            // 添加生成时间
            Paragraph timeInfo = new Paragraph("生成时间：" + LocalDateTime.now().format(DATE_FORMATTER))
                    .setFont(font)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginBottom(15);
            document.add(timeInfo);

            // 创建表格
            float[] columnWidths = new float[fields.size()];
            Arrays.fill(columnWidths, 1f);
            Table table = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();

            Map<String, String> fieldNameMap = getFeedbackFieldNameMap();

            // 添加表头
            DeviceRgb headerBgColor = new DeviceRgb(66, 139, 202);
            for (String field : fields) {
                Cell headerCell = new Cell()
                        .add(new Paragraph(fieldNameMap.getOrDefault(field, field))
                                .setFont(font)
                                .setFontSize(9)
                                .setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(headerBgColor)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(5);
                table.addHeaderCell(headerCell);
            }

            // 添加数据行
            boolean isOddRow = false;
            DeviceRgb oddRowColor = new DeviceRgb(245, 245, 245);

            for (FeedbackData feedback : feedbackList) {
                for (String field : fields) {
                    String value = getFeedbackFieldValue(feedback, field);
                    Cell dataCell = new Cell()
                            .add(new Paragraph(value != null && !value.isEmpty() ? value : "-")
                                    .setFont(font)
                                    .setFontSize(8))
                            .setTextAlignment(TextAlignment.CENTER)
                            .setPadding(4);

                    if (isOddRow) {
                        dataCell.setBackgroundColor(oddRowColor);
                    }
                    table.addCell(dataCell);
                }
                isOddRow = !isOddRow;
            }

            document.add(table);

            // 添加统计信息
            Paragraph stats = new Paragraph("共 " + feedbackList.size() + " 条记录")
                    .setFont(font)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(15);
            document.add(stats);

        } catch (Exception e) {
            log.error("PDF导出失败", e);
            throw new IOException("PDF导出失败: " + e.getMessage(), e);
        } finally {
            document.close();
        }
    }

    /**
     * 获取反馈记录字段值
     */
    private String getFeedbackFieldValue(FeedbackData feedback, String field) {
        try {
            switch (field) {
                case "feedbackId":
                    return feedback.getFeedbackId() != null ? feedback.getFeedbackId().toString() : "";
                case "userId":
                    return feedback.getUserId() != null ? feedback.getUserId().toString() : "";
                case "recordId":
                    return feedback.getRecordId() != null ?  feedback.getRecordId().toString() : "";
                case "originalResult":
                    return feedback.getOriginalResult() != null ? feedback.getOriginalResult().toString() : "";
                case "correctResult":
                    return feedback.getCorrectResult() != null ?  feedback.getCorrectResult().toString() : "";
                case "feedbackType":
                    return feedback.getFeedbackType() != null ? getFeedbackTypeText(feedback.getFeedbackType().name()) : "";
                case "feedbackReason":
                    return feedback.getFeedbackReason() != null ? feedback.getFeedbackReason() : "";
                case "qualityScore":
                    return feedback.getQualityScore() != null ? feedback.getQualityScore().toString() : "";
                case "status":
                    return feedback.getStatus() != null ? getStatusText(feedback.getStatus().name()) : "";
                case "modelName":
                    if (feedback.getRecordId() != null) {
                        return recordRepository.findById(feedback.getRecordId())
                                .flatMap(record -> record.getModelId() != null ?
                                        modelRepository.findById(record.getModelId()) : Optional.empty())
                                .map(m -> m.getModelName())
                                .orElse("");
                    }
                    return "";
                case "modelVersion":
                    if (feedback.getRecordId() != null) {
                        return recordRepository.findById(feedback.getRecordId())
                                .flatMap(record -> record.getModelId() != null ?
                                        modelRepository.findById(record.getModelId()) : Optional.empty())
                                .map(m -> m.getModelVersion())
                                .orElse("");
                    }
                    return "";
                case "reviewerId":
                    return feedback.getReviewerId() != null ? feedback.getReviewerId().toString() : "";
                case "reviewNote":
                    return feedback.getReviewNote() != null ? feedback.getReviewNote() : "";
                case "createTime":
                    return feedback.getCreateTime() != null ? feedback.getCreateTime().format(DATE_FORMATTER) : "";
                case "reviewTime":
                    return feedback.getReviewTime() != null ?  feedback.getReviewTime().format(DATE_FORMATTER) : "";
                default:
                    return "";
            }
        } catch (Exception e) {
            log.warn("获取字段值失败: field={}, error={}", field, e.getMessage());
            return "";
        }
    }

    /**
     * 获取反馈字段名称映射
     */
    private Map<String, String> getFeedbackFieldNameMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("feedbackId", "反馈ID");
        map.put("userId", "用户ID");
        map.put("recordId", "记录ID");
        map.put("originalResult", "原始结果");
        map.put("correctResult", "正确结果");
        map.put("feedbackType", "反馈类型");
        map.put("feedbackReason", "反馈原因");
        map.put("qualityScore", "质量评分");
        map.put("status", "状态");
        map.put("modelName", "模型名称");
        map.put("modelVersion", "模型版本");
        map.put("reviewerId", "审核人ID");
        map.put("reviewNote", "审核备注");
        map.put("createTime", "提交时间");
        map.put("reviewTime", "审核时间");
        return map;
    }

    /**
     * 获取默认反馈导出字段
     */
    private List<String> getDefaultFeedbackFields() {
        return Arrays.asList("feedbackId", "userId", "recordId", "originalResult", "correctResult",
                "feedbackType", "feedbackReason", "qualityScore", "status", "modelName", "modelVersion", "createTime");
    }

    // ==================== 工具方法 ====================

    /**
     * 获取中文字体 - 多种方式尝试
     */
    private PdfFont getChineseFont() throws IOException {
        // 方式1: 尝试从 classpath 加载自定义字体文件
        try {
            ClassPathResource fontResource = new ClassPathResource("fonts/simsun.ttf");
            if (fontResource.exists()) {
                byte[] fontBytes = fontResource.getInputStream().readAllBytes();
                return PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            }
        } catch (Exception e) {
            log.debug("从classpath加载字体失败，尝试其他方式: {}", e.getMessage());
        }

        // 方式2: 尝试加载系统字体 (Windows)
        String[] windowsFonts = {
                "C:/Windows/Fonts/simsun.ttc,0",
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/msyh.ttc,0",
                "C:/Windows/Fonts/simfang.ttf"
        };
        for (String fontPath : windowsFonts) {
            try {
                return PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            } catch (Exception e) {
                log.debug("加载Windows字体失败: {}", fontPath);
            }
        }

        // 方式3: 尝试加载系统字体 (Linux)
        String[] linuxFonts = {
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc,0",
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc,0",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
                "/usr/share/fonts/truetype/arphic/uming.ttc,0"
        };
        for (String fontPath : linuxFonts) {
            try {
                return PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            } catch (Exception e) {
                log.debug("加载Linux字体失败: {}", fontPath);
            }
        }

        // 方式4: 尝试加载系统字体 (macOS)
        String[] macFonts = {
                "/System/Library/Fonts/PingFang.ttc,0",
                "/System/Library/Fonts/STHeiti Light.ttc,0",
                "/Library/Fonts/Songti.ttc,0"
        };
        for (String fontPath : macFonts) {
            try {
                return PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            } catch (Exception e) {
                log.debug("加载macOS字体失败: {}", fontPath);
            }
        }

        // 方式5: 尝试使用 iText 内置的亚洲字体
        try {
            return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_NOT_EMBEDDED);
        } catch (Exception e) {
            log.debug("加载STSong-Light字体失败: {}", e.getMessage());
        }

        // 方式6: 使用默认字体（不支持中文，但至少能显示英文和数字）
        log.warn("无法加载中文字体，将使用默认字体（中文可能显示为乱码）");
        return PdfFontFactory.createFont();
    }

    /**
     * 解析字段列表
     */
    private List<String> parseFields(String fieldsStr, List<String> defaultFields) {
        if (fieldsStr == null || fieldsStr.trim().isEmpty()) {
            return defaultFields;
        }
        return Arrays.asList(fieldsStr.split(","));
    }

    /**
     * 设置Excel响应头
     */
    private void setExcelResponseHeaders(HttpServletResponse response, String filename) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encodedFilename = URLEncoder.encode(filename + ".xlsx", StandardCharsets.UTF_8.toString());
        response.setHeader("Content-Disposition", "attachment; filename=" + encodedFilename);
    }

    /**
     * 设置CSV响应头
     */
    private void setCsvResponseHeaders(HttpServletResponse response, String filename) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        String encodedFilename = URLEncoder.encode(filename + ".csv", StandardCharsets.UTF_8.toString());
        response.setHeader("Content-Disposition", "attachment; filename=" + encodedFilename);
    }

    /**
     * 设置PDF响应头
     */
    private void setPdfResponseHeaders(HttpServletResponse response, String filename) throws IOException {
        response.setContentType("application/pdf");
        response.setCharacterEncoding("UTF-8");
        String encodedFilename = URLEncoder.encode(filename + ".pdf", StandardCharsets.UTF_8.toString());
        response.setHeader("Content-Disposition", "attachment; filename=" + encodedFilename);
    }

    /**
     * 创建Excel标题样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建Excel数据样式
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 获取输入方式文本
     */
    private String getInputTypeText(String inputType) {
        switch (inputType) {
            case "CANVAS":
            case "MULTI":
                return "手写板";
            case "UPLOAD":
                return "图片上传";
            case "CAMERA":
                return "相机拍摄";
            default:
                return "未知";
        }
    }

    /**
     * 获取反馈类型文本
     */
    private String getFeedbackTypeText(String feedbackType) {
        switch (feedbackType) {
            case "WRONG_RESULT":
                return "识别错误";
            case "LOW_CONFIDENCE":
                return "置信度低";
            case "OTHER":
                return "其他";
            default:
                return "未知";
        }
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(String status) {
        switch (status) {
            case "PENDING":
                return "待审核";
            case "ACCEPTED":
                return "已接受";
            case "REJECTED":
                return "已拒绝";
            default:
                return "未知";
        }
    }
}