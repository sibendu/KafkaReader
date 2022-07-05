package com.intel.model;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "intel_metric")
@Getter @Setter @NoArgsConstructor
public class IntelMetric {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	
	private Date timestamp;
	private String metricName;	
	private Double metricValue;	
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serial_no")
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JsonIgnore
    private IntelObject intelObject;

	public IntelMetric(Date timestamp, String metricName, Double metricValue, IntelObject intelObject) {
		super();
		this.timestamp = timestamp;
		this.metricName = metricName;
		this.metricValue = metricValue;
		this.intelObject = intelObject;
	}
}
