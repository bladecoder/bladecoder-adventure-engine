package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Jump an Ink knot or stich.")
public class InkRunAction implements Action {
	@ActionPropertyDescription("The knot/stich/function path to jump. Ej: 'myKnotName' or 'myKnotName.theStitchWithin'")
	@ActionProperty(required = true)
	private String path;

	@ActionPropertyDescription("List of params (comma separated) for the path. Use the % prefix for numbers.")
	@ActionProperty(required = false)
	private String params;

	@ActionPropertyDescription("The conversation flow. Empty selects the default flow.")
	@ActionProperty(required = false)
	private String flow;

	@ActionProperty(required = true)
	@ActionPropertyDescription("Waits for the action to finish.")
	private boolean wait = true;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		try {
			Object[] p = null;

			if (params != null && !params.trim().isEmpty()) {
				String[] split = params.split(",");

				p = new Object[split.length];

				for (int i = 0; i < split.length; i++) {
					String v = split[i].trim();
					Object val = v;

					if (v.charAt(0) == '%') {
						try {
							val = Integer.parseInt(v.substring(1));
						} catch (NumberFormatException e) {
							// do nothing
						}
					}

					p[i] = val;
				}
			}

			w.getInkManager().runPath(path, p, flow, wait ? cb : null);
		} catch (Exception e) {
			EngineLogger.error("Cannot jump to: " + path + " " + e.getMessage());
		}

		return wait;
	}
}
