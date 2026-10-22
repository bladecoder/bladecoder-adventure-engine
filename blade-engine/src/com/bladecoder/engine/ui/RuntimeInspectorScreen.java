package com.bladecoder.engine.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.bladecoder.engine.remote.RemoteControlServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Shipped runtime inspector; activated explicitly and backed by the common debug service. */
public class RuntimeInspectorScreen implements BladeScreen {
    private UI ui;
    private Stage stage;
    private com.badlogic.gdx.graphics.Texture background;
    private Label output,status;
    private SelectBox<String> domain,operation;
    private TextField arguments,scenarioPath,queryPath;
    private String pending;
    private long lastPoll;
    public void setUI(UI ui){this.ui=ui;}
    private RemoteControlServer server(){return ui.getRemoteControlServer();}
    @Override public void show() {
        stage=new Stage(new ScreenViewport());Gdx.input.setInputProcessor(stage);
        Table layout=new Table(ui.getSkin());layout.setFillParent(true);layout.pad(12);stage.addActor(layout);
        com.badlogic.gdx.graphics.Pixmap pixel=new com.badlogic.gdx.graphics.Pixmap(1,1,com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        try{pixel.setColor(.04f,.04f,.04f,.97f);pixel.fill();background=new com.badlogic.gdx.graphics.Texture(pixel);}finally{pixel.dispose();}
        layout.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(background)));
        domain=new SelectBox<String>(ui.getSkin());domain.setName("inspector.domain");domain.setItems("state","events","dialogue","actions","content","scenarios");
        layout.add(button("Back","inspector.back",new Task(){public void run(){ui.setCurrentScreen(ui.getWorld().getCurrentScene()==null?UI.Screens.MENU_SCREEN:UI.Screens.SCENE_SCREEN);}})).width(90);
        layout.add(domain).expandX().fillX();
        layout.add(button("Refresh","inspector.refresh",new Task(){public void run() throws Exception{refresh();}})).width(120);
        layout.add(button("Capture","inspector.capture",new Task(){public void run() throws Exception{queue("capture","{\"kind\":\"ui\"}");}})).width(120);
        layout.add(button("Export","inspector.export",new Task(){public void run() throws Exception{status.setText("Exported: "+server().exportInspectorEvidence().get("directory"));}})).width(110);
        layout.row();
        queryPath=new TextField("",ui.getSkin());queryPath.setName("inspector.query");queryPath.setMessageText("JSON pointer within selected view, e.g. /scene/id or /ink/variables");
        layout.add(queryPath).colspan(5).expandX().fillX();layout.row();
        output=new Label("",ui.getSkin(),"debug");output.setName("inspector.output");output.setWrap(true);
        ScrollPane scroll=new ScrollPane(output,ui.getSkin());scroll.setFadeScrollBars(false);scroll.setScrollingDisabled(true,false);
        layout.add(scroll).colspan(5).expand().fill().padTop(8).padBottom(8);layout.row();
        operation=new SelectBox<String>(ui.getSkin());operation.setName("inspector.operation");
        operation.setItems("capture","gameplay","scenario.run","clock.freeze","fixture.seed","fixture.actor","fixture.variable","content.status","content.preview","content.activate","content.export","content.rollback","content.reset");
        arguments=new TextField("{}",ui.getSkin());arguments.setName("inspector.arguments");
        layout.add(operation).colspan(2).width(240);layout.add(arguments).colspan(2).expandX().fillX();
        layout.add(button("Run","inspector.run",new Task(){public void run() throws Exception{queue(operation.getSelected(),arguments.getText());}})).width(100);layout.row();
        scenarioPath=new TextField("",ui.getSkin());scenarioPath.setName("inspector.scenario");scenarioPath.setMessageText("Absolute path to scenario JSON");
        layout.add(scenarioPath).colspan(4).expandX().fillX();
        layout.add(button("Replay","inspector.replay",new Task(){public void run() throws Exception{
            String path=scenarioPath.getText();if(!new java.io.File(path).isAbsolute())throw new IllegalArgumentException("Choose an absolute scenario JSON filename");
            com.badlogic.gdx.files.FileHandle file=Gdx.files.absolute(path);
            if(!file.exists() || file.isDirectory() || file.length()>60000)throw new IllegalArgumentException("Scenario file is unavailable or exceeds the command limit");
            queue("scenario.run","{\"scenario\":"+file.readString("UTF-8")+"}");
        }})).width(110);layout.row();
        status=new Label("Explicit debug controls. Content changes require restart.",ui.getSkin(),"debug");status.setName("inspector.status");status.setWrap(true);
        layout.add(status).colspan(5).fillX().height(50);
        domain.addListener(new ChangeListener(){public void changed(ChangeEvent event,Actor actor){try{refresh();}catch(Exception failure){status.setText(failure.toString());}}});
        layout.addListener(new InputListener(){public boolean keyUp(InputEvent event,int key){if(key==Input.Keys.ESCAPE){ui.setCurrentScreen(ui.getWorld().getCurrentScene()==null?UI.Screens.MENU_SCREEN:UI.Screens.SCENE_SCREEN);return true;}return false;}});
        stage.setKeyboardFocus(layout);
        try{refresh();}catch(Exception failure){status.setText(failure.toString());}
    }
    private interface Task{void run() throws Exception;}
    private TextButton button(String label,String name,final Task task) {
        TextButton.TextButtonStyle style=new TextButton.TextButtonStyle(ui.getSkin().get(TextButton.TextButtonStyle.class));
        style.font=ui.getSkin().get("debug",Label.LabelStyle.class).font;
        TextButton button=new TextButton(label,style);button.setName(name);
        button.addListener(new ChangeListener(){public void changed(ChangeEvent event,Actor actor){try{task.run();}catch(Exception failure){if(status!=null)status.setText(failure.toString());}}});return button;
    }
    private void queue(String name,String args) throws Exception {
        if(pending!=null)throw new IllegalStateException("Wait for the active inspector command");
        Map<String,Object> job=server().submitInspectorCommand(name,args);pending=(String)job.get("command_id");status.setText("Accepted: "+name+" / "+pending);
    }
    private void refresh() throws Exception {
        String selected=domain.getSelected();Object data;
        if(selected.equals("events"))data=server().inspectEvents();
        else {
            Map<String,Object> state=server().inspectDebug();data=state;
            if(selected.equals("content"))data=state.get("content_overrides");
            else if(selected.equals("scenarios"))data=state.get("scenario_runs");
            else if(selected.equals("dialogue")) {
                java.util.Map<String,Object> text=new java.util.LinkedHashMap<String,Object>();
                text.put("text",state.get("text"));text.put("choices",state.get("choices"));text.put("ink",state.get("ink"));data=text;
            } else if(selected.equals("actions")) {
                List<Object> active=new ArrayList<Object>();
                collect(state.get("scene_verbs"),active);
                Object scene=state.get("scene");if(scene instanceof Map) {
                    Object actors=((Map<?,?>)scene).get("actors");if(actors instanceof Iterable)for(Object actor:(Iterable<?>)actors)collect(((Map<?,?>)actor).get("verbs"),active);
                }
                Object ink=state.get("ink");if(ink instanceof Map)collect(((Map<?,?>)ink).get("runners"),active);data=active;
            }
        }
        data=com.bladecoder.engine.remote.RemoteInspectorQuery.select(data,queryPath.getText());
        String json=new com.badlogic.gdx.utils.JsonReader().parse(server().debugJson(data)).prettyPrint(com.badlogic.gdx.utils.JsonWriter.OutputType.json,100);
        output.setText(json.length()>24000?json.substring(0,24000)+"\n[Display truncated; export retains full snapshot.]":json);
    }
    private void collect(Object runners,List<Object> output) {
        if(runners instanceof Iterable)for(Object runner:(Iterable<?>)runners)if(runner instanceof Map && Boolean.TRUE.equals(((Map<?,?>)runner).get("active")))output.add(runner);
    }
    @Override public void render(float delta) {
        Gdx.gl.glClearColor(.05f,.05f,.05f,1);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        // Inspection does not silently stop world simulation while reporting paused=false.
        if(ui.getWorld().getCurrentScene()!=null && ui.getWorld().getAssetState()==com.bladecoder.engine.model.World.AssetState.LOADED)
            ui.getScreen(UI.Screens.SCENE_SCREEN).render(delta);
        if(stage==null)return;
        stage.act(delta);stage.draw();
        if(pending!=null && System.nanoTime()-lastPoll>150000000L) {
            lastPoll=System.nanoTime();
            try {
                Map<String,Object> job=server().inspectCommand(pending);String state=(String)job.get("status");
                if(!state.equals("accepted") && !state.equals("running") && !state.equals("cancelling")) {
                    String message=state+": "+server().debugJson(job.get("error")!=null?job.get("error"):job.get("result"));
                    status.setText(message.length()>800?message.substring(0,800)+" [see exported command receipt]":message);pending=null;
                }
            } catch(Exception failure){status.setText(failure.toString());pending=null;}
        }
    }
    @Override public void resize(int w,int h){if(stage!=null)stage.getViewport().update(w,h,true);}
    @Override public void hide(){dispose();}
    @Override public void dispose(){if(stage!=null){stage.dispose();stage=null;}if(background!=null){background.dispose();background=null;}}
    @Override public void pause(){}
    @Override public void resume(){}
}
