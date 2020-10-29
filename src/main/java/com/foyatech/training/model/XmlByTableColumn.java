package com.foyatech.training.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "column")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlByTableColumn implements Serializable{

	private static final long serialVersionUID = 1L;

	@XmlAttribute(name = "name")
	private String name;

	@XmlAttribute(name = "type")
	private String type;

	@XmlAttribute(name = "value")
	private String value;
}
