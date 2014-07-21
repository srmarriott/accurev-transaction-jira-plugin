package com.github.srmarriott.jira.plugins.accurev.action;


import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.github.srmarriott.jira.plugins.accurev.AccuRevDepotManager;
import com.github.srmarriott.jira.plugins.accurev.MultipleAccuRevDepotManager;

/**
 * Manage 1 or more depots
 */
public class ViewAccuRevDepotsAction extends AccuRevActionSupport
{

    public ViewAccuRevDepotsAction(MultipleAccuRevDepotManager manager) {
        super (manager);
    }

    public Collection<AccuRevDepotManager> getDepots() {
        List<AccuRevDepotManager> accuRevDepotManagers = new ArrayList<AccuRevDepotManager>(getMultipleAccuRevDepotManager().getDepotList());

        Collections.sort(
            accuRevDepotManagers,
            new Comparator<AccuRevDepotManager>() {
                public int compare(AccuRevDepotManager left, AccuRevDepotManager right) {
                    return StringUtils.defaultString(left.getDepotName()).compareTo(
                            StringUtils.defaultString(right.getDepotName())
                    );
                }
            }
        );

        return accuRevDepotManagers;
    }
}