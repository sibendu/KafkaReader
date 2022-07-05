package com.intel.model;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(name = "intel_object")
public class IntelObject {
	
	@Id
	private String serialNo;

	private String objectId;

	private String objectType;
	
	public IntelObject(String serialNo, String objectId, String objectType) {
		super();
		this.serialNo = serialNo;
		this.objectId = objectId;
		this.objectType = objectType;
		this.metrics = metrics;
	}

	@OneToMany(mappedBy = "intelObject", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
	private Set<IntelMetric> metrics = new HashSet<>();
	
	public void addMetric(IntelMetric metric) {
		this.metrics.add(metric);
	}
		
}
