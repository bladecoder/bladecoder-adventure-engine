package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

import java.util.List;

@ActionDescription("Marks the end of a block for a control action")
public class EndAction extends AbstractControlAction {

    @Override
    public void init(World w) {
    }

    @Override
    public boolean run(VerbRunner cb) {
        // FIXME: This is now more generic than before, but also less optimized (we always get our "parent")
        final List<Action> actions = cb.getActions();
        final int ip = cb.getIP();

        final int parentIp = getParentControlAction(caID, actions, ip);
        final AbstractControlAction parent = (AbstractControlAction) actions.get(parentIp);

        if (parent instanceof RepeatAction) {
            cb.setIP(parentIp - 1);
        } else if (parent instanceof AbstractIfAction) {
            int newIp = skipControlIdBlock(actions, parentIp); // goto Else
            newIp = skipControlIdBlock(actions, newIp); // goto EndIf

            cb.setIP(newIp);
        }

        return false;
    }

    private int getParentControlAction(String caID, List<Action> actions, int ip) {
        do {
            ip--;
        } while (!(actions.get(ip) instanceof AbstractControlAction) || !((AbstractControlAction) actions.get(
                ip)).getControlActionID().equals(caID));

        return ip;
    }
}
