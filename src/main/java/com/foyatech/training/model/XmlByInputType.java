package com.foyatech.training.model;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "input")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlByInputType implements Serializable{

	private static final long serialVersionUID = 1L;

	@XmlAttribute(name = "type")
	private String inputType;

	@XmlAttribute(name = "delimiter")
	private String delimiter;

	@XmlElement(name = "column")
	public List<XmlByInputColumn> cloumn;

}
