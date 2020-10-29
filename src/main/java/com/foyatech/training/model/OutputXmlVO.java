package com.foyatech.training.model;

import lombok.Data;

@Data
public class OutputXmlVO {
	
	private String name;
	private String type;
	private int length;
	private String delimiter;
	private boolean nullable;
	private boolean quoted;
	
}
