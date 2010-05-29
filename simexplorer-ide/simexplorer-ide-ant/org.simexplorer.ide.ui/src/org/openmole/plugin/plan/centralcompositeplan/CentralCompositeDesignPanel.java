/*
 *  Copyright (c) 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.openmole.plugin.plan.centralcomposite;

import java.util.Enumeration;
import javax.swing.AbstractButton;
import org.openide.util.lookup.ServiceProvider;
import org.simexplorer.core.workflow.methods.EditorPanel;

@ServiceProvider(service=EditorPanel.class)
public class CentralCompositeDesignPanel extends EditorPanel<CentralCompositePlan> {

    /** Creates new form LHSPlanPanel */
    public CentralCompositeDesignPanel() {
        super(CentralCompositePlan.class);
        initComponents();
    }

    @Override
    public void applyChanges() {
        super.applyChanges();
        getObjectEdited().setNbCenterPoint((Integer)nbCenterPointsjSpinner.getValue());
        getObjectEdited().setNbReplicatesForAxialPoint((Integer)replicationAxialPointsjSpinner.getValue());
        getObjectEdited().setNbReplicatesForFactorialPoint((Integer)replicationFactorialPointsjSpinner.getValue());

        int i=0;
        Enumeration<AbstractButton> but=buttonGroup1.getElements();
        while (but.hasMoreElements()){
            if (but.nextElement().isSelected()) {
                //getObjectEdited().setAlpha(getObjectEdited().getAlphan(i));
                break;
            }
            i++;
        }
    }

    @Override
    public void setObjectEdited(CentralCompositePlan method) {
        super.setObjectEdited(method);
        nbCenterPointsjSpinner.setValue(getObjectEdited().getNbCenterPoint());
        replicationAxialPointsjSpinner.setValue(getObjectEdited().getNbReplicatesForAxialPoint());
        replicationFactorialPointsjSpinner.setValue(getObjectEdited().getNbReplicatesForFactorialPoint());

        int i=0;
        /*jLabelRotatable.setText(((Double)getObjectEdited().getAlphan(i++)).toString());
        jLabelSphericale.setText(((Double)getObjectEdited().getAlphan(i++)).toString());
        jLabelPracticale.setText(((Double)getObjectEdited().getAlphan(i++)).toString());
        jLabelFaceCentered.setText(((Double)getObjectEdited().getAlphan(i++)).toString());*/
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        replicationFactorialPointsjSpinner = new javax.swing.JSpinner();
        replicationAxialPointsjSpinner = new javax.swing.JSpinner();
        nbCenterPointsjSpinner = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jRadioButton3 = new javax.swing.JRadioButton();
        jRadioButton4 = new javax.swing.JRadioButton();
        jRadioButton5 = new javax.swing.JRadioButton();
        jLabelRotatable = new javax.swing.JLabel();
        jLabelSphericale = new javax.swing.JLabel();
        jLabelPracticale = new javax.swing.JLabel();
        jLabelFaceCentered = new javax.swing.JLabel();
        jTextFieldOther = new javax.swing.JTextField();

        setBorder(javax.swing.BorderFactory.createTitledBorder("Central Composite Design (CCD) options"));
        setName("Central Composite design (CCD) options"); // NOI18N

        jLabel2.setFont(new java.awt.Font("DejaVu Sans", 1, 12));
        jLabel2.setText("Replication");

        jLabel3.setText("Replication of factorial points");

        jLabel4.setText("Replicates of axial (star) points");

        jLabel5.setText("number of center points");

        jLabel1.setFont(new java.awt.Font("DejaVu Sans", 1, 12));
        jLabel1.setText("Alpha");

        buttonGroup1.add(jRadioButton1);
        jRadioButton1.setText("rotatable");
        jRadioButton1.setMaximumSize(new java.awt.Dimension(82, 15));
        jRadioButton1.setMinimumSize(new java.awt.Dimension(82, 15));
        jRadioButton1.setPreferredSize(new java.awt.Dimension(82, 15));

        buttonGroup1.add(jRadioButton2);
        jRadioButton2.setText("sphericale");
        jRadioButton2.setMaximumSize(new java.awt.Dimension(82, 15));
        jRadioButton2.setMinimumSize(new java.awt.Dimension(82, 15));
        jRadioButton2.setPreferredSize(new java.awt.Dimension(82, 15));

        buttonGroup1.add(jRadioButton3);
        jRadioButton3.setText("practicale");
        jRadioButton3.setMaximumSize(new java.awt.Dimension(82, 15));
        jRadioButton3.setMinimumSize(new java.awt.Dimension(82, 15));
        jRadioButton3.setPreferredSize(new java.awt.Dimension(82, 15));

        buttonGroup1.add(jRadioButton4);
        jRadioButton4.setText("face centered");
        jRadioButton4.setMaximumSize(new java.awt.Dimension(82, 15));
        jRadioButton4.setMinimumSize(new java.awt.Dimension(82, 15));
        jRadioButton4.setPreferredSize(new java.awt.Dimension(82, 15));

        buttonGroup1.add(jRadioButton5);
        jRadioButton5.setText("other");
        jRadioButton5.setMaximumSize(new java.awt.Dimension(82, 15));
        jRadioButton5.setMinimumSize(new java.awt.Dimension(82, 15));
        jRadioButton5.setPreferredSize(new java.awt.Dimension(82, 15));

        jLabelRotatable.setText("...");

        jLabelSphericale.setText("...");

        jLabelPracticale.setText("...");

        jLabelFaceCentered.setText("...");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jRadioButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 133, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(73, 73, 73)
                        .addComponent(jTextFieldOther, javax.swing.GroupLayout.DEFAULT_SIZE, 66, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jRadioButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 133, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 74, Short.MAX_VALUE)
                        .addComponent(jLabelFaceCentered, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jRadioButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 111, Short.MAX_VALUE)
                        .addComponent(jLabelPracticale, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jRadioButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 111, Short.MAX_VALUE)
                        .addComponent(jLabelSphericale, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jRadioButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 125, Short.MAX_VALUE)
                        .addComponent(jLabelRotatable, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 89, Short.MAX_VALUE)
                        .addComponent(nbCenterPointsjSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 55, Short.MAX_VALUE)
                                .addComponent(replicationFactorialPointsjSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 47, Short.MAX_VALUE)
                                .addComponent(replicationAxialPointsjSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addGap(24, 24, 24))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel3))
                    .addComponent(replicationFactorialPointsjSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(replicationAxialPointsjSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(nbCenterPointsjSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelRotatable))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelSphericale))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelPracticale))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelFaceCentered))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 6, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldOther, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabelFaceCentered;
    private javax.swing.JLabel jLabelPracticale;
    private javax.swing.JLabel jLabelRotatable;
    private javax.swing.JLabel jLabelSphericale;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JRadioButton jRadioButton4;
    private javax.swing.JRadioButton jRadioButton5;
    private javax.swing.JTextField jTextFieldOther;
    private javax.swing.JSpinner nbCenterPointsjSpinner;
    private javax.swing.JSpinner replicationAxialPointsjSpinner;
    private javax.swing.JSpinner replicationFactorialPointsjSpinner;
    // End of variables declaration//GEN-END:variables

}
