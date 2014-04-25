#ifdef GL_ES 
#define LOWP lowp
#define MED mediump
#define HIGH highp
precision mediump float;
#else
#define MED
#define LOWP
#define HIGH
#endif


#ifdef normalFlag
varying vec3 v_normal;
#endif //normalFlag

#if defined(colorFlag)
varying vec4 v_color;
#endif


#if defined(diffuseTextureFlag)
#define textureFlag
varying MED vec2 v_texCoords0;
#endif

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef lightingFlag
varying vec3 v_lightDiffuse;

#if	defined(ambientLightFlag)
#define ambientFlag
#endif //ambientFlag

#if defined(ambientFlag) && defined(separateAmbientFlag)
varying vec3 v_ambientLight;
#endif //separateAmbientFlag

#if defined(numPointLights) && (numPointLights > 0)
struct PointLight
{
	vec3 color;
	vec3 position;
};
uniform PointLight u_pointLights[numPointLights];
#endif // numPointLights

#endif //lightingFlag

void main() {
		
	//#if defined(diffuseTextureFlag) && defined(diffuseColorFlag) && defined(colorFlag)
	//	vec4 diffuse = texture2D(u_diffuseTexture, v_texCoords0) * u_diffuseColor * v_color;
	//#elif defined(diffuseTextureFlag) && defined(diffuseColorFlag)
	//	vec4 diffuse = texture2D(u_diffuseTexture, v_texCoords0) * u_diffuseColor;
	//#elif defined(diffuseTextureFlag) && defined(colorFlag)
	//	vec4 diffuse = texture2D(u_diffuseTexture, v_texCoords0) * v_color;
	#if defined(diffuseTextureFlag)
		vec4 diffuse = texture2D(u_diffuseTexture, v_texCoords0);
	#elif defined(diffuseColorFlag) && defined(colorFlag)
		vec4 diffuse = u_diffuseColor * v_color;
	#elif defined(diffuseColorFlag)
		vec4 diffuse = u_diffuseColor;
	#elif defined(colorFlag)
		vec4 diffuse = v_color;
	#else
		vec4 diffuse = vec4(1.0);
	#endif

	#if (!defined(lightingFlag) || numPointLights < 1)  
		gl_FragColor = diffuse;
	#else
				
		float intensity;
		vec4 c;
		intensity = dot(normalize(u_pointLights[0].position), normalize(v_normal));
		
		c = diffuse;
		
		c.rgb *= u_pointLights[0].color;

		if (intensity < 0.2)
			c.rgb = (intensity * 2.0) * diffuse.rgb;
	
		gl_FragColor = c;
	#endif					


}
