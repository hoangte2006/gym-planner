package com.gym.member.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.github.lgooddatepicker.components.DatePicker;
import com.gym.core.ui.DatePickers;
import com.gym.core.ui.Dialogs;
import com.gym.member.service.MemberService;

public class DailyPassDialog extends JDialog {

    private final MemberService memberService;
    private final JTextField txtLastName = new JTextField(18);
    private final JTextField txtFirstName = new JTextField(18);
    private final JTextField txtPhone = new JTextField(18);
    private final JLabel lblPhoneError = new JLabel(" ");
    private final DatePicker dpDate = DatePickers.date();
    private final JTextField txtRate = new JTextField("15.00", 18);
    private boolean success = false;

    public DailyPassDialog(Window owner, MemberService service) {
        super(owner, "Bán vé ngày (Daily Pass)", ModalityType.APPLICATION_MODAL);
        this.memberService = service;

        initUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 10, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        c.gridx = 0; c.gridy = row; form.add(new JLabel("Họ *:"), c);
        c.gridx = 1; form.add(txtLastName, c);
        row++;

        c.gridx = 0; c.gridy = row; form.add(new JLabel("Tên *:"), c);
        c.gridx = 1; form.add(txtFirstName, c);
        row++;

        lblPhoneError.setForeground(Color.RED);
        lblPhoneError.setFont(lblPhoneError.getFont().deriveFont(11f));
        c.gridx = 0; c.gridy = row; form.add(new JLabel("Số điện thoại *:"), c);
        c.gridx = 1; form.add(txtPhone, c);
        row++;
        c.gridx = 1; c.gridy = row; form.add(lblPhoneError, c);
        row++;

        dpDate.setDate(LocalDate.now());
        c.gridx = 0; c.gridy = row; form.add(new JLabel("Ngày tập:"), c);
        c.gridx = 1; form.add(dpDate, c);
        row++;

        txtRate.setEditable(false);
        c.gridx = 0; c.gridy = row; form.add(new JLabel("Giá vé ($):"), c);
        c.gridx = 1; form.add(txtRate, c);

        add(form, BorderLayout.CENTER);

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnPay = new JButton("Bán vé & Thu tiền");
        JButton btnCancel = new JButton("Đóng");
        pnlBtns.add(btnPay);
        pnlBtns.add(btnCancel);
        add(pnlBtns, BorderLayout.SOUTH);

        btnPay.addActionListener(e -> submit());
        btnCancel.addActionListener(e -> dispose());

        txtPhone.getDocument().addDocumentListener(new DocumentListener() {
            @Override 
            public void insertUpdate(DocumentEvent e) { checkPhone(); }
            @Override
            public void removeUpdate(DocumentEvent e) { checkPhone(); }
            @Override
            public void changedUpdate(DocumentEvent e) { checkPhone(); }
        });
    }

    private void checkPhone() {
        String p = txtPhone.getText().trim();
        if (p.isEmpty()) {
            txtPhone.setBorder(new JTextField().getBorder());
            lblPhoneError.setText(" ");
        } else if (!p.matches("^0\\d{9}$")) {
            txtPhone.setBorder(new LineBorder(Color.RED, 1));
            lblPhoneError.setText("SĐT phải gồm 10 chữ số, bắt đầu bằng 0");
        } else {
            txtPhone.setBorder(new JTextField().getBorder());
            lblPhoneError.setText(" ");
        }
    }

    private void submit() {
        try {
            memberService.sellDailyPass(
                    txtFirstName.getText(),
                    txtLastName.getText(),
                    txtPhone.getText(),
                    dpDate.getDate(),
                    new BigDecimal(txtRate.getText().trim())
            );
            success = true;
            Dialogs.info(this, "Bán vé thành công và đã tạo hóa đơn $15.00!");
            dispose();
        } catch (Exception ex) {
            Dialogs.error(this, ex);
        }
    }

    public boolean isSuccess() {
        return success;
    }
}