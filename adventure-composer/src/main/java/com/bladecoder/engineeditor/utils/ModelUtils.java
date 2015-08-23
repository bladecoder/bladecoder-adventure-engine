package com.bladecoder.engineeditor.utils;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.actions.ModelDescription;
import com.bladecoder.engine.actions.ModelPropertyType;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.model.ModelTypeLink;
import com.bladecoder.engineeditor.ui.components.EditableSelectBox;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;
import com.bladecoder.engineeditor.ui.components.LinkableInputPanel;
import com.bladecoder.engineeditor.ui.components.OptionsInputPanel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModelUtils {
	public static String getInfo(@Nonnull Class<?> clazz) {
		ModelDescription modelDescription = clazz.getAnnotation(ModelDescription.class);
		if (modelDescription == null) {
			throw new RuntimeException(clazz.getName() + " doesn't seem to be annotated with @ModelDescription");
		}
		return modelDescription.value();
	}

	@Nonnull
	public static List<Param> getParams(@Nonnull Class<?> clazz) {
		final List<Param> params = new ArrayList<>();

		// Used to insert parent classes properties at the top
		int idx;
		while (clazz != null && clazz != Object.class) {
			idx = 0;
			for (Field field : clazz.getDeclaredFields()) {
				final JsonProperty property = field.getAnnotation(JsonProperty.class);
				if (property == null) {
					continue;
				}
				final ModelPropertyType propertyType = field.getAnnotation(ModelPropertyType.class);
				final Param.Type type;
				if (propertyType != null) {
					type = propertyType.value();
				} else {
					type = typeFromFieldType(field);
					if (type == null) {
						throw new RuntimeException(clazz.getName() + '.' + field.getName() + " doesn't seem to be annotated with @ModelPropertyType, and its type can't be determined automatically");
					}
				}
				final JsonPropertyDescription propertyDescription = field.getAnnotation(JsonPropertyDescription.class);
				if (propertyDescription == null) {
					throw new RuntimeException(clazz.getName() + '.' + field.getName() + " doesn't seem to be annotated with @JsonPropertyDescription");
				}

				final String name = property.value() == null || property.value().trim().isEmpty() ? field.getName() : property.value();

				Object[] options = null;
				if (field.getType().isEnum()) {
					final boolean accessible = field.isAccessible();
					field.setAccessible(true);
					options = field.getType().getEnumConstants();
					field.setAccessible(accessible);
				}
				if (options == null && type == Param.Type.OPTION) {
					options = optionsFromFieldType(field);
					if (options == null) {
						throw new RuntimeException(clazz.getName() + '.' + field.getName() + " is an OPTION type, but we can't find suitable options for it");
					}
				}

				final ModelTypeLink modelTypeLink = field.getAnnotation(ModelTypeLink.class);
				final String link = modelTypeLink != null ? modelTypeLink.value() : null;

				params.add(idx++, new Param(
						name, formatName(name), propertyDescription.value(), type,
						property.required(), property.defaultValue(),
						options, link));
			}
			clazz = clazz.getSuperclass();
		}
		return params;
	}

	public static Collection<InputPanel> getInputsFromModelClass(List<Param> params, Skin skin) {
		Map<String, InputPanel> parameters = new LinkedHashMap<>(params.size());

		for (final Param param : params) {
			final InputPanel parameter = InputPanelFactory.createInputPanel(skin, param);

			parameters.put(param.getId(), parameter);

			if ((parameter.getField() instanceof TextField && param.getName().toLowerCase().endsWith("text")) ||
					parameter.getField() instanceof ScrollPane) {
				parameter.getCell(parameter.getField()).fillX();
			}
		}
		for (Param param : params) {
			String link = param.getLink();
			if (link != null) {

				InputPanel listeningInputPanel = parameters.get(param.getId());
				if (!(listeningInputPanel instanceof LinkableInputPanel)) {
					throw new RuntimeException("Param '" + param.getId() + "' has a link to '" + link + "' but '" + param.getId() + "' is not a LinkableInputPanel");
				}
				InputPanel parentInputPanel = parameters.get(link);
				if (!(parentInputPanel instanceof OptionsInputPanel)) {
					throw new RuntimeException("Param '" + param.getId() + "' has a link to '" + link + "' but '" + link + "' is not a OptionsInputPanel");
				}
				((OptionsInputPanel)parentInputPanel).addChangeListener(new ChangeListener() {
					@Override
					public void changed(ChangeEvent event, Actor actor) {
						final String selected;
						if (actor instanceof SelectBox) {
							// FIXME: After we change everything to be a bean, this won't be a String anymore
							selected = ((SelectBox<String>) actor).getSelected();
						} else if (actor instanceof EditableSelectBox) {
							selected = ((EditableSelectBox) actor).getSelected();
						} else {
							return;
						}
						((LinkableInputPanel)listeningInputPanel).linkChanged(selected);
					}
				});
			}
		}

		return parameters.values();
	}

	private static String formatName(String name) {
		StringBuilder sb = new StringBuilder();
		char[] charArray = name.toCharArray();
		for (int j = 0; j < charArray.length; j++) {
			char c = charArray[j];

			if (Character.isUpperCase(c) && j != 0) {
				sb.append(' ');
				sb.append(Character.toLowerCase(c));
			} else if (j == 0) {
				sb.append(Character.toUpperCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private static Object[] optionsFromFieldType(Field field) {
		final Class<?> type = field.getType();
		// FIXME: This can miss values, as converters can be registered thorugh other means
		final JsonSubTypes jsonSubTypes = type.getAnnotation(JsonSubTypes.class);
		if (jsonSubTypes == null) {
			return null;
		}

		final List<String> result = new ArrayList<>();
		for (JsonSubTypes.Type jsonType : jsonSubTypes.value()) {
			String name = jsonType.name();
			if (name.isEmpty()) {
				final JsonTypeName jsonTypeName = jsonType.value().getAnnotation(JsonTypeName.class);
				if (jsonTypeName == null) {
					throw new RuntimeException(jsonType.value().getName() + " is a JSON SubType, but it doesn't have a @JsonTypeName or a name on its parent (" + type.getName() + ")");
				}
				name = jsonTypeName.value();
			}
			result.add(name);
		}

		return result.toArray(new String[result.size()]);
	}

	private static Param.Type typeFromFieldType(Field field) {
		final Class<?> type = field.getType();
		if (type == String.class) {
			return null;        // Strings have special meaning, so they always need an explicit annotation
		} else if (type == Boolean.class || type == boolean.class) {
			return Param.Type.BOOLEAN;
		} else if (type.isEnum()) {
			return Param.Type.OPTION;
		} else if (type == Integer.class || type == int.class) {
			return Param.Type.INTEGER;
		} else if (type == Float.class || type == float.class) {
			return Param.Type.FLOAT;
		} else if (type == Vector2.class) {
			return Param.Type.VECTOR2;
		} else if (type == Vector3.class) {
			return Param.Type.VECTOR3;
		}
		return null;
	}
}
