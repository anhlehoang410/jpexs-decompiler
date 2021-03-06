/*
 *  Copyright (C) 2010-2015 JPEXS
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jpexs.decompiler.flash.gui.abc;

import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.abc.ScriptPack;
import com.jpexs.decompiler.flash.abc.types.traits.Trait;
import com.jpexs.decompiler.flash.abc.types.traits.TraitClass;
import com.jpexs.decompiler.flash.gui.AppStrings;
import com.jpexs.decompiler.flash.timeline.AS3Package;
import com.jpexs.decompiler.flash.treeitems.AS3ClassTreeItem;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class ClassesListTreeModel extends AS3ClassTreeItem implements TreeModel {

    private SWF swf;

    private List<ScriptPack> list;

    private AS3Package root;

    private final List<TreeModelListener> listeners = new ArrayList<>();

    public List<ScriptPack> getList() {
        return list;
    }

    public ClassesListTreeModel(SWF swf) {
        this(swf, null);
    }

    public ClassesListTreeModel(SWF swf, String filter) {
        super(null, null);
        root = new AS3Package(null, swf);
        this.swf = swf;
        this.list = swf.getAS3Packs();
        setFilter(filter);
    }

    @Override
    public SWF getSwf() {
        return swf;
    }

    public final void update() {
        this.list = swf.getAS3Packs();
        TreeModelEvent event = new TreeModelEvent(this, new TreePath(root));
        for (TreeModelListener listener : listeners) {
            listener.treeStructureChanged(event);
        }
    }

    public final void setFilter(String filter) {
        root.clear();

        filter = (filter == null || filter.isEmpty()) ? null : filter.toLowerCase();
        for (ScriptPack item : list) {
            if (filter != null) {
                if (!item.getClassPath().toString().toLowerCase().contains(filter)) {
                    continue;
                }
            }

            String packageStr = item.getClassPath().packageStr;
            AS3Package pkg = ensurePackage(packageStr);
            pkg.addScriptPack(item);
        }
    }

    private AS3Package ensurePackage(String packageStr) {
        StringTokenizer st = new StringTokenizer(packageStr, ".");
        AS3Package parent = root;
        while (st.hasMoreTokens()) {
            String pathElement = st.nextToken();
            AS3Package pkg = parent.getSubPackage(pathElement);
            if (pkg == null) {
                pkg = new AS3Package(pathElement, swf);
                parent.addSubPackage(pkg);
            }

            parent = pkg;
        }

        return parent;
    }

    public ScriptPack getElementByClassIndex(int classIndex) {
        return getElementByClassIndexRecursive(root, classIndex);
    }

    private ScriptPack getElementByClassIndexRecursive(AS3Package item, int classIndex) {
        for (AS3Package pkg : item.getSubPackages()) {
            ScriptPack result = getElementByClassIndexRecursive(pkg, classIndex);
            if (result != null) {
                return result;
            }
        }

        for (ScriptPack sc : item.getScriptPacks()) {
            for (Trait t : sc.abc.script_info.get(sc.scriptIndex).traits.traits) {
                if (t instanceof TraitClass) {
                    if (((TraitClass) t).class_info == classIndex) {
                        return sc;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public AS3ClassTreeItem getRoot() {
        return root;
    }

    public List<AS3ClassTreeItem> getAllChildren(Object parent) {
        AS3Package pkg = (AS3Package) parent;
        return pkg.getAllChildren();
    }

    @Override
    public AS3ClassTreeItem getChild(Object parent, int index) {
        AS3Package pkg = (AS3Package) parent;
        return pkg.getChild(index);
    }

    @Override
    public int getChildCount(Object parent) {
        AS3ClassTreeItem parentItem = (AS3ClassTreeItem) parent;
        if (parentItem instanceof ScriptPack) {
            return 0;
        }

        AS3Package pkg = (AS3Package) parentItem;
        return pkg.getChildCount();
    }

    @Override
    public boolean isLeaf(Object node) {
        AS3ClassTreeItem te = (AS3ClassTreeItem) node;
        return te instanceof ScriptPack;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        AS3Package pkg = (AS3Package) parent;
        return pkg.getIndexOfChild((AS3ClassTreeItem) child);
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    @Override
    public String toString() {
        return AppStrings.translate("node.scripts");
    }
}
