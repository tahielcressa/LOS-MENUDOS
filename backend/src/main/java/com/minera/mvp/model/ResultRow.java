package com.minera.mvp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "result_rows", indexes = @Index(name = "idx_rows_run", columnList = "run_id"))
public class ResultRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id")
    private ProcessRun run;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    private int rowNumber;

    private LocalDate fecha;

    private String turno;

    private String zona;

    private String equipmentCode;

    private String equipmentName;

    private String equipmentArea;

    private String equipoTipo;

    private String estado;

    private BigDecimal tonelaje;

    private BigDecimal leyCu;

    private boolean valid;

    @Column(length = 1000)
    private String errors;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProcessRun getRun() { return run; }
    public void setRun(ProcessRun run) { this.run = run; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }
    public String getZona() { return zona; }
    public void setZona(String zona) { this.zona = zona; }
    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    public String getEquipmentArea() { return equipmentArea; }
    public void setEquipmentArea(String equipmentArea) { this.equipmentArea = equipmentArea; }
    public String getEquipoTipo() { return equipoTipo; }
    public void setEquipoTipo(String equipoTipo) { this.equipoTipo = equipoTipo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getTonelaje() { return tonelaje; }
    public void setTonelaje(BigDecimal tonelaje) { this.tonelaje = tonelaje; }
    public BigDecimal getLeyCu() { return leyCu; }
    public void setLeyCu(BigDecimal leyCu) { this.leyCu = leyCu; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getErrors() { return errors; }
    public void setErrors(String errors) { this.errors = errors; }
}