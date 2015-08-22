package com.bladecoder.engine.model;

import com.bladecoder.engine.actions.ModelPropertyType;
import com.bladecoder.engine.actions.Param;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public abstract class AbstractModel {
	@JsonProperty(required = true)
	@JsonPropertyDescription("The id of the element. Ids can only contain letters, numbers or the symbol '$'.")
	@ModelPropertyType(Param.Type.STRING)
	@NotNull
	@Pattern(regexp = "[a-zA-Z0-9$]")
	@Size(min=2)
	protected String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
