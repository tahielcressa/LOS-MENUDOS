package com.minera.mvp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "process_runs")
public class ProcessRun {

    public static final String STATUS_RUNNING = "EJECUTANDO";
    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERROR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "upload_id")
    private Upload upload;

    private String runType;

    @Column(nullable = false)
    private String status;

    private int totalRows;
    private int validRows;
    private int invalidRows;

    private BigDecimal totalTonnage;
    private BigDecimal avgGrade;

    @Column(length = 8000)
    private String kpis;

    @Column(length = 8000)
    private String logs;

    private String excelFileName;
    private String pdfFileName;

    private Long durationMs;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "executed_by")
    private AppUser executedBy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @PrePersist
    void onCreate() {
        if (startedAt == null) startedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Upload getUpload() { return upload; }
    public void setUpload(Upload upload) { this.upload = upload; }
    public String getRunType() { return runType; }
    public void setRunType(String runType) { this.runType = runType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getValidRows() { return validRows; }
    public void setValidRows(int validRows) { this.validRows = validRows; }
    public int getInvalidRows() { return invalidRows; }
    public void setInvalidRows(int invalidRows) { this.invalidRows = invalidRows; }
    public BigDecimal getTotalTonnage() { return totalTonnage; }
    public void setTotalTonnage(BigDecimal totalTonnage) { this.totalTonnage = totalTonnage; }
    public BigDecimal getAvgGrade() { return avgGrade; }
    public void setAvgGrade(BigDecimal avgGrade) { this.avgGrade = avgGrade; }
    public String getKpis() { return kpis; }
    public void setKpis(String kpis) { this.kpis = kpis; }
    public String getLogs() { return logs; }
    public void setLogs(String logs) { this.logs = logs; }
    public String getExcelFileName() { return excelFileName; }
    public void setExcelFileName(String excelFileName) { this.excelFileName = excelFileName; }
    public String getPdfFileName() { return pdfFileName; }
    public void setPdfFileName(String pdfFileName) { this.pdfFileName = pdfFileName; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public AppUser getExecutedBy() { return executedBy; }
    public void setExecutedBy(AppUser executedBy) { this.executedBy = executedBy; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}