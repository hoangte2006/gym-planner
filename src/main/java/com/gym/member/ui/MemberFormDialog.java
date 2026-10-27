package com.gym.member.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.github.lgooddatepicker.components.DatePicker;
import com.gym.core.model.Member;
import com.gym.core.ui.DatePickers;
import com.gym.core.ui.Dialogs;
import com.gym.member.service.MemberService;

public class MemberFormDialog extends JDialog {

    private final MemberService memberService;
    private final Member editingMember;
    private boolean saved = false;

    private final JTextField txtLastName = new JTextField(20);
    private final JTextField txtFirstName = new JTextField(20);
    private final JTextField txtPhone = new JTextField(20);
    private final JLabel lblPhoneError = new JLabel(" ");
    private final JTextField txtEmail = new JTextField(20);
    private final DatePicker dpBirthDate = DatePickers.date();
    private final JLabel lblAvatar = new JLabel("Chưa có ảnh", SwingConstants.CENTER);
    private byte[] photoBytes = null;

    public MemberFormDialog(Window owner, MemberService service, Member memberToEdit) {
        super(owner, memberToEdit == null ? "Thêm hội viên mới" : "Chỉnh sửa hội viên", ModalityType.APPLICATION_MODAL);
        this.memberService = service;
        this.editingMember = memberToEdit;

        initUI();
        bindData();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(12, 12));
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
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

        c.gridx = 0; c.gridy = row; form.add(new JLabel("Email:"), c);
        c.gridx = 1; form.add(txtEmail, c);
        row++;

        c.gridx = 0; c.gridy = row; form.add(new JLabel("Ngày sinh:"), c);
        c.gridx = 1; form.add(dpBirthDate, c);
        row++;

        lblAvatar.setPreferredSize(new Dimension(100, 100));
        lblAvatar.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JButton btnChoosePhoto = new JButton("Chọn ảnh...");
        btnChoosePhoto.addActionListener(e -> choosePhoto());

        JPanel pnlPhoto = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlPhoto.add(lblAvatar);
        pnlPhoto.add(btnChoosePhoto);

        c.gridx = 0; c.gridy = row; form.add(new JLabel("Ảnh thẻ:"), c);
        c.gridx = 1; form.add(pnlPhoto, c);

        add(form, BorderLayout.CENTER);

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnSave = new JButton("Lưu");
        JButton btnCancel = new JButton("Hủy");
        pnlBtns.add(btnSave);
        pnlBtns.add(btnCancel);
        add(pnlBtns, BorderLayout.SOUTH);

        btnSave.addActionListener(e -> save());
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
        String phone = txtPhone.getText().trim();
        if (phone.isEmpty()) {
            txtPhone.setBorder(new JTextField().getBorder());
            lblPhoneError.setText(" ");
        } else if (!phone.matches("^0\\d{9}$")) {
            txtPhone.setBorder(new LineBorder(Color.RED, 1));
            lblPhoneError.setText("Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0");
        } else {
            txtPhone.setBorder(new JTextField().getBorder());
            lblPhoneError.setText(" ");
        }
    }

    private void choosePhoto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Ảnh (*.jpg, *.png)", "jpg", "png", "jpeg"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(img, "jpg", baos);
                    photoBytes = baos.toByteArray();
                    lblAvatar.setIcon(new ImageIcon(img.getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
                    lblAvatar.setText("");
                }
            } catch (java.io.IOException ex) {
                Dialogs.error(this, ex);
            }
        }
    }

    private void bindData() {
        if (editingMember != null) {
            txtLastName.setText(editingMember.getLastName());
            txtFirstName.setText(editingMember.getFirstName());
            txtPhone.setText(editingMember.getPhone());
            txtEmail.setText(editingMember.getEmail());
            dpBirthDate.setDate(editingMember.getBirthDate());

            byte[] photo = memberService.getMemberPhoto(editingMember.getVisitorId());
            if (photo != null && photo.length > 0) {
                photoBytes = photo;
                try {
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(photo));
                    lblAvatar.setIcon(new ImageIcon(img.getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
                    lblAvatar.setText("");
                } catch (java.io.IOException e) {
                    lblAvatar.setText("Ảnh lỗi");
                }
            } else {
                lblAvatar.setText("Thiếu ảnh");
            }
        }
    }

    private void save() {
        try {
            Member m = editingMember != null ? editingMember : new Member();
            m.setLastName(txtLastName.getText().trim());
            m.setFirstName(txtFirstName.getText().trim());
            m.setPhone(txtPhone.getText().trim());
            m.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
            m.setBirthDate(dpBirthDate.getDate());
            if (photoBytes != null) {
                m.setPhoto(photoBytes);
            }

            memberService.saveMember(m);
            saved = true;
            dispose();
        } catch (Exception ex) {
            Dialogs.error(this, ex);
        }
    }

    public boolean isSaved() {
        return saved;
    }
}