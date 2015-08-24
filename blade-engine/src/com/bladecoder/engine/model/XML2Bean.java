package com.bladecoder.engine.model;

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.AnimationAction;
import com.bladecoder.engine.actions.CameraAction;
import com.bladecoder.engine.actions.CancelVerbAction;
import com.bladecoder.engine.actions.ChooseAction;
import com.bladecoder.engine.actions.DropItemAction;
import com.bladecoder.engine.actions.EndAction;
import com.bladecoder.engine.actions.GotoAction;
import com.bladecoder.engine.actions.IfAttrAction;
import com.bladecoder.engine.actions.IfPropertyAction;
import com.bladecoder.engine.actions.IfSceneAttrAction;
import com.bladecoder.engine.actions.LeaveAction;
import com.bladecoder.engine.actions.LoadChapterAction;
import com.bladecoder.engine.actions.LookAtAction;
import com.bladecoder.engine.actions.MoveToSceneAction;
import com.bladecoder.engine.actions.MusicAction;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.actions.PickUpAction;
import com.bladecoder.engine.actions.PositionAction;
import com.bladecoder.engine.actions.PropertyAction;
import com.bladecoder.engine.actions.RemoveActorAction;
import com.bladecoder.engine.actions.RemoveInventoryItemAction;
import com.bladecoder.engine.actions.RepeatAction;
import com.bladecoder.engine.actions.RunOnceAction;
import com.bladecoder.engine.actions.RunVerbAction;
import com.bladecoder.engine.actions.SayAction;
import com.bladecoder.engine.actions.SayDialogAction;
import com.bladecoder.engine.actions.ScaleAction;
import com.bladecoder.engine.actions.SetActorAttrAction;
import com.bladecoder.engine.actions.SetCutmodeAction;
import com.bladecoder.engine.actions.SetDialogOptionAttrAction;
import com.bladecoder.engine.actions.SetSceneStateAction;
import com.bladecoder.engine.actions.SetStateAction;
import com.bladecoder.engine.actions.ShowInventoryAction;
import com.bladecoder.engine.actions.SoundAction;
import com.bladecoder.engine.actions.TalktoAction;
import com.bladecoder.engine.actions.TransitionAction;
import com.bladecoder.engine.actions.WaitAction;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @deprecated This needs to be removed as soon as we finish with the JSON deserialization work
 */
@Deprecated
public final class XML2Bean {
	private static final XmlMapper mapper = new XmlMapper();
	private static final ObjectMapper jsonMapper = new ObjectMapper();
	static {
		mapper.disable(
				MapperFeature.AUTO_DETECT_CREATORS,
				MapperFeature.AUTO_DETECT_FIELDS,
				MapperFeature.AUTO_DETECT_GETTERS,
				MapperFeature.AUTO_DETECT_IS_GETTERS,
				MapperFeature.AUTO_DETECT_SETTERS
				);
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);

		jsonMapper.disable(
				MapperFeature.AUTO_DETECT_CREATORS,
				MapperFeature.AUTO_DETECT_FIELDS,
				MapperFeature.AUTO_DETECT_GETTERS,
				MapperFeature.AUTO_DETECT_IS_GETTERS,
				MapperFeature.AUTO_DETECT_SETTERS
				);
		jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);

		mapper.enable(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME);
		mapper.setVisibility(mapper.getVisibilityChecker().with(JsonAutoDetect.Visibility.NONE));
		jsonMapper.setVisibility(jsonMapper.getVisibilityChecker().with(JsonAutoDetect.Visibility.NONE));

		mapper.enable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		jsonMapper.enable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

		SimpleModule module = new SimpleModule();
		module.addDeserializer(Vector2.class, new JsonDeserializer<Vector2>() {
			@Override
			public Vector2 deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
				return Param.parseVector2(p.getValueAsString());
			}
		});
		module.registerSubtypes(AnimationAction.class,
				CameraAction.class,
				CancelVerbAction.class,
				ChooseAction.class,
//				CustomAction.class,
				DropItemAction.class,
				EndAction.class,
				GotoAction.class,
				IfAttrAction.class,
				IfPropertyAction.class,
				IfSceneAttrAction.class,
				LeaveAction.class,
				LoadChapterAction.class,
				LookAtAction.class,
				MoveToSceneAction.class,
				MusicAction.class,
				PickUpAction.class,
				PositionAction.class,
				PropertyAction.class,
				RemoveActorAction.class,
				RemoveInventoryItemAction.class,
				RepeatAction.class,
				RunOnceAction.class,
				RunVerbAction.class,
				SayAction.class,
				SayDialogAction.class,
				ScaleAction.class,
				SetActorAttrAction.class,
				SetCutmodeAction.class,
				SetDialogOptionAttrAction.class,
				SetSceneStateAction.class,
				SetStateAction.class,
				ShowInventoryAction.class,
				SoundAction.class,
				TalktoAction.class,
				TransitionAction.class,
				WaitAction.class
		);
		mapper.registerModule(module);
		jsonMapper.registerModule(module);
	}

	public static <T> T deserialize(Node node, Class<T> clazz) {
		try {
			return mapper.readValue(getStringFromElement(node), clazz);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RuntimeException();
		}
	}

	public static <T> void fillElement(T bean, Element e) {
		Map<String, Object> map = jsonMapper.convertValue(bean, Map.class);
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			e.setAttribute(entry.getKey(), entry.getValue().toString());
		}
	}

	private static String getStringFromElement(Node node) {
		Document document = node.getOwnerDocument();
		DOMImplementationLS domImplLS = (DOMImplementationLS) document
				.getImplementation();
		LSSerializer serializer = domImplLS.createLSSerializer();
		return serializer.writeToString(node);
	}

	public static <T> T load(File file, Class<T> clazz) {
		try {
			return mapper.readValue(file, clazz);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static <T> void save(File file, T bean) throws IOException {
		mapper.writeValue(file, bean);
	}

	public static <T> T loadJson(File file, Class<T> clazz) throws IOException {
		return jsonMapper.readValue(file, clazz);
	}

	public static <T> void saveJson(File file, T bean) throws IOException {
		jsonMapper.writeValue(file, bean);
	}
}
