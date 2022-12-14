package utils.serialize.json;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.annotation.JSONType;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeBeanInfo;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.spi.Module;
import com.alibaba.fastjson.util.FieldInfo;
import com.alibaba.fastjson.util.TypeUtils;

import utils.provider.Provider;
import utils.provider.ProviderManager;

class JSONGlobalConfigurator implements JSONConfigurator {

	public static final JSONConfigurator INSTANCE = new JSONGlobalConfigurator();

	private static final ProviderManager pm = new ProviderManager();

	static final SerializeConfig SERIALIZE_CONFIG = SerializeConfig.getGlobalInstance();

	static final ParserConfig PARSER_CONFIG = ParserConfig.getGlobalInstance();

	private static final ToStringSerializer TO_STRING_SERIALIZER = new ToStringSerializer();

	private static final RuntimeDeserializer RUNTIME_DESERIALIZER = new RuntimeDeserializer();

	private static final SuperTypeSerializationModule SUPER_TYPE_SERIALIZATION_MODULE = SuperTypeSerializationModule.INSTANCE;

	private static final ProxyTypeConfigureModule PROXY_TYPE_SERIALIZATION_MODULE = ProxyTypeConfigureModule.INSTANCE;

	private static final DynamicTypeConfigureModule DYNAMIC_TYPE_SERIALIZATION_MODULE = DynamicTypeConfigureModule.INSTANCE;

	private static final Module[] GLOBAL_SERIALIZE_MODULES = { SUPER_TYPE_SERIALIZATION_MODULE,
			PROXY_TYPE_SERIALIZATION_MODULE, DYNAMIC_TYPE_SERIALIZATION_MODULE };

	private static final Module[] GLOBAL_DESERIALIZE_MODULES = { PROXY_TYPE_SERIALIZATION_MODULE };

	private static volatile boolean inited = false;

	static {
		initConfiguration();

		autoRegister(INSTANCE);
	}

	/**
	 * ???????????????????????????
	 * <p>
	 * 
	 * ???????????????????????? {@link JSONAutoConfigure} ????????????????????? SPI ???????????????<br>
	 * 
	 * ?????????????????????????????????????????? {@link JSONAutoConfigure} ?????????????????????
	 * 
	 * 
	 */
	synchronized static void initConfiguration() {
		if (inited) {
			return;
		}
		PARSER_CONFIG.setAutoTypeSupport(true);

		for (Module module : GLOBAL_SERIALIZE_MODULES) {
			SERIALIZE_CONFIG.register(module);
		}
		for (Module module : GLOBAL_DESERIALIZE_MODULES) {
			PARSER_CONFIG.register(module);
		}

		inited = true;
	}

	private static void autoRegister(JSONConfigurator configuration) {
		// ??????????????????????????????????????????????????????
		pm.installAllProviders(JSONAutoConfigure.class, JSONAutoConfigure.class.getClassLoader());
		// ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????ProviderManager ?????????????????????????????????
		pm.installAllProviders(JSONAutoConfigure.class, Thread.currentThread().getContextClassLoader());

		Iterable<Provider<JSONAutoConfigure>> providers = pm.getAllProviders(JSONAutoConfigure.class);
		for (Provider<JSONAutoConfigure> provider : providers) {
			register(provider, configuration);
		}
	}

	private static void register(Provider<JSONAutoConfigure> provider, JSONConfigurator configuration) {
		provider.getService().configure(configuration);
	}

	@Override
	public void configSuperSerializer(Class<?> superType, ObjectSerializer serializer) {
		SUPER_TYPE_SERIALIZATION_MODULE.registerSuperSerializer(superType, serializer);
	}

	@Override
	public void configProxyInterfaces(Class<?>... types) {
		PROXY_TYPE_SERIALIZATION_MODULE.register(types);
	}
	
	@Override
	public void registerDynamicTypeConverter(DynamicTypeConverter typeConverter) {
		DYNAMIC_TYPE_SERIALIZATION_MODULE.register(typeConverter);
	}

	@Override
	public void configDeserializeTypeMapping(Class<?> fromClazz, Class<?> toClazz) {
		RUNTIME_DESERIALIZER.addTypeMap(fromClazz, toClazz);
		PARSER_CONFIG.putDeserializer(fromClazz, RUNTIME_DESERIALIZER);
	}

	@Override
	public void configSerialization(Class<?> clazz, ObjectSerializer serializer, ObjectDeserializer deserializer) {
		SERIALIZE_CONFIG.put(clazz, serializer);
		PARSER_CONFIG.putDeserializer(clazz, deserializer);
	}

	@Override
	public void configSerializer(Class<?> clazz, ObjectSerializer serializer) {
		SERIALIZE_CONFIG.put(clazz, serializer);
	}

	@Override
	public void configDeserializer(Class<?> clazz, ObjectDeserializer deserializer) {
		PARSER_CONFIG.putDeserializer(clazz, deserializer);
	}

	/**
	 * ??????????????????????????????????????????????????? JSONBeanSerializer ???????????????
	 * 
	 * @param clazz
	 * @param enable
	 */
	@Deprecated
	public void configOutputTypeName(Class<?> clazz, boolean enable) {
		SERIALIZE_CONFIG.config(clazz, SerializerFeature.WriteClassName, enable);
	}

	/**
	 * ???????????????????????????????????????????????? {@link Object#toString()} ??????????????? ;
	 * 
	 * @param type
	 */
	@Override
	public void configStringSerializer(Class<?> type) {
		SERIALIZE_CONFIG.put(type, TO_STRING_SERIALIZER);
	}

	/**
	 * ???????????????????????????
	 * 
	 * <br>
	 * ?????????????????????
	 * 
	 * @param type
	 */
	@Override
	public void disableCircularReferenceDetect() {
		JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.DisableCircularReferenceDetect.getMask();
	}

	protected static SerializeBeanInfo buildBeanInfo(Class<?> beanType, boolean outputClassName) {
		Map<String, String> aliasMap = null;
		PropertyNamingStrategy propertyNamingStrategy = null;
		boolean fieldBased = false;

		JSONType jsonType = TypeUtils.getAnnotation(beanType, JSONType.class);
		String[] orders = null;
		int features;
		String typeName = null, typeKey = null;
		if (jsonType != null) {
			orders = jsonType.orders();

			typeName = jsonType.typeName();
			if (typeName.length() == 0) {
				typeName = null;
			}

			PropertyNamingStrategy jsonTypeNaming = jsonType.naming();
			if (jsonTypeNaming != PropertyNamingStrategy.CamelCase) {
				propertyNamingStrategy = jsonTypeNaming;
			}

			features = SerializerFeature.of(jsonType.serialzeFeatures());
			for (Class<?> supperClass = beanType.getSuperclass(); supperClass != null
					&& supperClass != Object.class; supperClass = supperClass.getSuperclass()) {
				JSONType superJsonType = TypeUtils.getAnnotation(supperClass, JSONType.class);
				if (superJsonType == null) {
					break;
				}
				typeKey = superJsonType.typeKey();
				if (typeKey.length() != 0) {
					break;
				}
			}

			for (Class<?> interfaceClass : beanType.getInterfaces()) {
				JSONType superJsonType = TypeUtils.getAnnotation(interfaceClass, JSONType.class);
				if (superJsonType != null) {
					typeKey = superJsonType.typeKey();
					if (typeKey.length() != 0) {
						break;
					}
				}
			}

			if (typeKey != null && typeKey.length() == 0) {
				typeKey = null;
			}
		} else {
			features = 0;
		}

		features = SerializerFeature.config(features, SerializerFeature.WriteClassName, outputClassName);

		// fieldName,field ????????????fieldName???????????????????????????findField?????????
		Map<String, Field> fieldCacheMap = new HashMap<String, Field>();
		ParserConfig.parserAllFieldToCache(beanType, fieldCacheMap);
		List<FieldInfo> fieldInfoList = fieldBased
				? TypeUtils.computeGettersWithFieldBase(beanType, aliasMap, false, propertyNamingStrategy) //
				: TypeUtils.computeGetters(beanType, jsonType, aliasMap, fieldCacheMap, false, propertyNamingStrategy);
		FieldInfo[] fields = new FieldInfo[fieldInfoList.size()];
		fieldInfoList.toArray(fields);
		FieldInfo[] sortedFields;
		List<FieldInfo> sortedFieldList;
		if (orders != null && orders.length != 0) {
			sortedFieldList = fieldBased
					? TypeUtils.computeGettersWithFieldBase(beanType, aliasMap, true, propertyNamingStrategy) //
					: TypeUtils.computeGetters(beanType, jsonType, aliasMap, fieldCacheMap, true,
							propertyNamingStrategy);
		} else {
			sortedFieldList = new ArrayList<FieldInfo>(fieldInfoList);
			Collections.sort(sortedFieldList);
		}
		sortedFields = new FieldInfo[sortedFieldList.size()];
		sortedFieldList.toArray(sortedFields);
		if (Arrays.equals(sortedFields, fields)) {
			sortedFields = fields;
		}
		return new SerializeBeanInfo(beanType, jsonType, typeName, typeKey, features, fields, sortedFields);
	}
}
