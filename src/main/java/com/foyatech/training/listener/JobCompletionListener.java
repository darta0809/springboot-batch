package com.foyatech.training.listener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;

import com.foyatech.training.model.XmlByInputColumn;
import com.foyatech.training.model.XmlByInputType;
import com.foyatech.training.model.XmlByOutputColumn;
import com.foyatech.training.model.XmlByOutputType;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class JobCompletionListener extends JobExecutionListenerSupport {

	XmlByInputType xmlByInputType = new XmlByInputType();
	XmlByOutputType xmlByOutputType = new XmlByOutputType();

	String cofingFile;
	String fileType;

	public JobCompletionListener(String configFile, String fileType) {
		this.cofingFile = configFile;
		this.fileType = fileType;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		if (jobExecution.getStatus() == BatchStatus.STARTED) {
			log.info("BATCH JOB STARTED SUCCESSFULLY");

			// 讀XML設定檔
			String xml_inputType = "";
			String xml_inputDelimiter = "";
			String xml_outputType = "";
			String xml_outputDelimiter = "";
			
			SAXReader reader = new SAXReader();
			File file = new File(cofingFile);
			InputStream in;
			Document document = null;
			
			try {
				in = new FileInputStream(file);
				document = reader.read(in);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (DocumentException e) {
				e.printStackTrace();
			}

			Element root = document.getRootElement();

			if (fileType.equals("CSV")) {
				// 讀設定檔欄位資訊 input
				xml_inputType = root.element("input").attributeValue("type");
				xml_inputDelimiter = root.element("input").attributeValue("delimiter");
				List<Element> listInputElement = root.element("input").elements();

				List<XmlByInputColumn> xmlByInputColumnList = new ArrayList<>();

				for (Element e1 : listInputElement) {

					XmlByInputColumn xmlByInputColumn = new XmlByInputColumn();

					Attribute nameAttribute = e1.attribute("name");
					String name = nameAttribute.getValue();
					xmlByInputColumn.setName(name);
					Attribute typeAttribute = e1.attribute("type");
					String type = typeAttribute.getValue();
					xmlByInputColumn.setType(type);
					Attribute lengthAttribute = e1.attribute("quoted");
					String quoted = lengthAttribute.getValue();
					xmlByInputColumn.setQuoted(quoted);
					Attribute nullableAttribute = e1.attribute("nullable");
					String nullable = nullableAttribute.getValue();
					xmlByInputColumn.setNullable(nullable);

					xmlByInputColumnList.add(xmlByInputColumn);

				}

				xmlByInputType.setInputType(xml_inputType);
				xmlByInputType.setDelimiter(xml_inputDelimiter);
				xmlByInputType.setCloumn(xmlByInputColumnList);

				// 讀設定檔欄位資訊 output
				xml_outputType = root.element("output").attributeValue("type");

				List<Element> listOutputElement = root.element("output").elements();

				List<XmlByOutputColumn> xmlByOutputColumnList = new ArrayList<>();

				for (Element e1 : listOutputElement) {

					XmlByOutputColumn xmlByOutputColumn = new XmlByOutputColumn();

					Attribute nameAttribute = e1.attribute("name");
					String name = nameAttribute.getValue();
					xmlByOutputColumn.setName(name);
					Attribute typeAttribute = e1.attribute("type");
					String type = typeAttribute.getValue();
					xmlByOutputColumn.setType(type);
					Attribute quotedAttribute = e1.attribute("length");
					String length = quotedAttribute.getValue();
					xmlByOutputColumn.setLength(length);

					xmlByOutputColumnList.add(xmlByOutputColumn);
				}

				xmlByOutputType.setOutputType(xml_outputType);
				xmlByOutputType.setCloumn(xmlByOutputColumnList);

				// 讀FY_TB_TRANSACTION的設定

				jobExecution.getExecutionContext().put("xmlByInputType", xmlByInputType);
				jobExecution.getExecutionContext().put("xmlByOutputType", xmlByOutputType);
			}
			
			if(fileType.equals("FIX")) {
				
				// 讀設定檔欄位資訊 input
				xml_inputType = root.element("input").attributeValue("type");
				
				List<Element> listInputElement = root.element("input").elements();

				List<XmlByInputColumn> xmlByInputColumnList = new ArrayList<>();

				for (Element e1 : listInputElement) {

					XmlByInputColumn xmlByInputColumn = new XmlByInputColumn();

					Attribute nameAttribute = e1.attribute("name");
					String name = nameAttribute.getValue();
					xmlByInputColumn.setName(name);
					Attribute typeAttribute = e1.attribute("type");
					String type = typeAttribute.getValue();
					xmlByInputColumn.setType(type);
					Attribute lengthAttribute = e1.attribute("length");
					String length = lengthAttribute.getValue();
					xmlByInputColumn.setLength(length);
					Attribute nullableAttribute = e1.attribute("nullable");
					String nullable = nullableAttribute.getValue();
					xmlByInputColumn.setNullable(nullable);

					xmlByInputColumnList.add(xmlByInputColumn);

				}

				xmlByInputType.setInputType(xml_inputType);
				xmlByInputType.setCloumn(xmlByInputColumnList);

				// 讀設定檔欄位資訊 output
				xml_outputType = root.element("output").attributeValue("type");
				xml_outputDelimiter = root.element("output").attributeValue("delimiter");
				List<Element> listOutputElement = root.element("output").elements();

				List<XmlByOutputColumn> xmlByOutputColumnList = new ArrayList<>();

				for (Element e1 : listOutputElement) {

					XmlByOutputColumn xmlByOutputColumn = new XmlByOutputColumn();

					Attribute nameAttribute = e1.attribute("name");
					String name = nameAttribute.getValue();
					xmlByOutputColumn.setName(name);
					Attribute typeAttribute = e1.attribute("type");
					String type = typeAttribute.getValue();
					xmlByOutputColumn.setType(type);
					Attribute quotedAttribute = e1.attribute("quoted");
					String quoted = quotedAttribute.getValue();
					xmlByOutputColumn.setQuoted(quoted);

					xmlByOutputColumnList.add(xmlByOutputColumn);
				}

				xmlByOutputType.setOutputType(xml_outputType);
				xmlByOutputType.setDelimiter(xml_outputDelimiter);
				xmlByOutputType.setCloumn(xmlByOutputColumnList);

				// 讀FY_TB_TRANSACTION的設定

				jobExecution.getExecutionContext().put("xmlByInputType", xmlByInputType);
				jobExecution.getExecutionContext().put("xmlByOutputType", xmlByOutputType);
			
			}
		}
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		if (jobExecution.getStatus() == BatchStatus.COMPLETED || jobExecution.getStatus() == BatchStatus.FAILED) {
			log.info("BATCH JOB " + jobExecution.getStatus());
		}
	}

}
