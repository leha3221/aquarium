package ru.mirea.aquarium.fx;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import ru.mirea.aquarium.model.*;
import ru.mirea.aquarium.repository.*;
import ru.mirea.aquarium.service.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

public class ManagementController {
    @FXML private TabPane tabs;
    @FXML private TableView<Client> clientsTable;
    @FXML private TableView<Aquarium> aquariumsTable;
    @FXML private TableView<Fish> fishTable;

    private final ClientRepository clientRepository = new ClientRepository();
    private final AquariumRepository aquariumRepository = new AquariumRepository();
    private final ClientService clientService = new ClientService(clientRepository);
    private final AquariumService aquariumService = new AquariumService(aquariumRepository, clientRepository);
    private final FishService fishService = new FishService(new FishRepository(), aquariumRepository);

    @FXML private void initialize() {
        col(clientsTable, "ID", c -> Long.toString(c.getId()));
        col(clientsTable, "ФИО", Client::getFullName);
        col(clientsTable, "Телефон", Client::getPhone);
        col(clientsTable, "Email", Client::getEmail);
        col(clientsTable, "Адрес", Client::getAddress);

        col(aquariumsTable, "ID", a -> Long.toString(a.getId()));
        col(aquariumsTable, "Владелец", a -> clientName(a.getClientId()));
        col(aquariumsTable, "Название", Aquarium::getName);
        col(aquariumsTable, "Тип", a -> typeName(a.getType()));
        col(aquariumsTable, "Объём, л", a -> a.getVolumeLiters().toPlainString());

        col(fishTable, "ID", f -> Long.toString(f.getId()));
        col(fishTable, "Аквариум", f -> aquariumName(f.getAquariumId()));
        col(fishTable, "Вид", Fish::getSpecies);
        col(fishTable, "Количество", f -> Integer.toString(f.getQuantity()));
        refresh();
    }

    private <T> void col(TableView<T> table, String title, Function<T, String> text) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(text.apply(data.getValue())));
        column.setPrefWidth(title.equals("Адрес") ? 230 : 145);
        table.getColumns().add(column);
    }

    private String clientName(long id) {
        return clientRepository.findById(id).map(Client::getFullName).orElse("Клиент №" + id);
    }
    private String aquariumName(long id) {
        return aquariumRepository.findById(id).map(Aquarium::getName).orElse("Аквариум №" + id);
    }
    private String typeName(AquariumType type) {
        return switch (type) {
            case FRESHWATER -> "Пресноводный";
            case MARINE -> "Морской";
            case REPTILE -> "Для рептилий";
            case PLANT -> "Растительный";
        };
    }

    @FXML private void refresh() {
        try {
            clientsTable.setItems(FXCollections.observableArrayList(clientService.findAll()));
            aquariumsTable.setItems(FXCollections.observableArrayList(aquariumService.findAll()));
            fishTable.setItems(FXCollections.observableArrayList(fishService.findAll()));
        } catch (Exception e) { showError(e); }
    }

    @FXML private void add() { editEntity(null); }
    @FXML private void edit() {
        Object selected = switch (tabs.getSelectionModel().getSelectedIndex()) {
            case 0 -> clientsTable.getSelectionModel().getSelectedItem();
            case 1 -> aquariumsTable.getSelectionModel().getSelectedItem();
            default -> fishTable.getSelectionModel().getSelectedItem();
        };
        if (selected == null) { info("Сначала выберите запись в таблице."); return; }
        editEntity(selected);
    }

    private void editEntity(Object current) {
        try {
            switch (tabs.getSelectionModel().getSelectedIndex()) {
                case 0 -> editClient((Client) current);
                case 1 -> editAquarium((Aquarium) current);
                default -> editFish((Fish) current);
            }
        } catch (Exception e) { showError(e); }
    }

    private void editClient(Client old) {
        TextField name = new TextField(old == null ? "" : old.getFullName());
        TextField phone = new TextField(old == null ? "" : old.getPhone());
        TextField email = new TextField(old == null ? "" : old.getEmail());
        TextField address = new TextField(old == null ? "" : old.getAddress());
        GridPane form = form(new String[]{"ФИО", "Телефон", "Email", "Адрес"}, name, phone, email, address);
        if (!showForm(old == null ? "Новый клиент" : "Изменить клиента", form)) return;
        Client client = new Client(old == null ? 0 : old.getId(), name.getText().trim(), phone.getText().trim(), email.getText().trim(), address.getText().trim());
        if (old == null) clientService.create(client); else clientService.update(client);
        refresh();
    }

    private void editAquarium(Aquarium old) {
        List<Client> clients = clientService.findAll();
        if (clients.isEmpty()) { info("Сначала создайте клиента на вкладке «Клиенты»."); return; }
        ComboBox<Client> owner = new ComboBox<>(FXCollections.observableArrayList(clients));
        owner.setConverter(converter(c -> c.getFullName() + " (#" + c.getId() + ")"));
        TextField name = new TextField(old == null ? "" : old.getName());
        ComboBox<AquariumType> type = new ComboBox<>(FXCollections.observableArrayList(AquariumType.values()));
        type.setConverter(converter(this::typeName));
        TextField volume = new TextField(old == null ? "100" : old.getVolumeLiters().toPlainString());
        if (old == null) { owner.getSelectionModel().selectFirst(); type.setValue(AquariumType.FRESHWATER); }
        else {
            owner.setValue(clients.stream().filter(c -> c.getId() == old.getClientId()).findFirst().orElse(null));
            type.setValue(old.getType());
        }
        GridPane form = form(new String[]{"Владелец", "Название", "Тип", "Объём (л)"}, owner, name, type, volume);
        if (!showForm(old == null ? "Новый аквариум" : "Изменить аквариум", form)) return;
        if (owner.getValue() == null || type.getValue() == null) throw new IllegalArgumentException("Выберите владельца и тип аквариума.");
        Aquarium aquarium = new Aquarium(old == null ? 0 : old.getId(), owner.getValue().getId(), name.getText().trim(), type.getValue(), new BigDecimal(volume.getText().trim()));
        if (old == null) aquariumService.create(aquarium); else aquariumService.update(aquarium);
        refresh();
    }

    private void editFish(Fish old) {
        List<Aquarium> aquariums = aquariumService.findAll().stream().filter(a -> a.getType() != AquariumType.REPTILE).toList();
        if (aquariums.isEmpty()) { info("Сначала создайте аквариум, подходящий для рыб."); return; }
        ComboBox<Aquarium> aquarium = new ComboBox<>(FXCollections.observableArrayList(aquariums));
        aquarium.setConverter(converter(a -> a.getName() + " (#" + a.getId() + ")"));
        TextField species = new TextField(old == null ? "" : old.getSpecies());
        TextField quantity = new TextField(old == null ? "1" : Integer.toString(old.getQuantity()));
        if (old == null) aquarium.getSelectionModel().selectFirst();
        else aquarium.setValue(aquariums.stream().filter(a -> a.getId() == old.getAquariumId()).findFirst().orElse(null));
        GridPane form = form(new String[]{"Аквариум", "Вид рыбы", "Количество"}, aquarium, species, quantity);
        if (!showForm(old == null ? "Добавить рыб" : "Изменить запись о рыбах", form)) return;
        if (aquarium.getValue() == null) throw new IllegalArgumentException("Выберите аквариум.");
        Fish fish = new Fish(old == null ? 0 : old.getId(), aquarium.getValue().getId(), species.getText().trim(), Integer.parseInt(quantity.getText().trim()));
        if (old == null) fishService.create(fish); else fishService.update(fish);
        refresh();
    }

    @FXML private void delete() {
        int index = tabs.getSelectionModel().getSelectedIndex();
        Object selected = switch (index) {
            case 0 -> clientsTable.getSelectionModel().getSelectedItem();
            case 1 -> aquariumsTable.getSelectionModel().getSelectedItem();
            default -> fishTable.getSelectionModel().getSelectedItem();
        };
        if (selected == null) { info("Сначала выберите запись в таблице."); return; }
        String warning = switch (index) {
            case 0 -> "Удаление клиента также удалит его заявки, аквариумы и рыб.";
            case 1 -> "Удаление аквариума также удалит связанные записи о рыбах.";
            default -> "Удалить выбранную запись о рыбах?";
        };
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, warning, ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Подтвердите удаление");
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            if (selected instanceof Client c) clientService.delete(c.getId());
            else if (selected instanceof Aquarium a) aquariumService.delete(a.getId());
            else if (selected instanceof Fish f) fishService.delete(f.getId());
            refresh();
        } catch (Exception e) { showError(e); }
    }

    private GridPane form(String[] labels, Node... fields) {
        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(10); grid.setPrefWidth(450);
        for (int i = 0; i < fields.length; i++) {
            grid.add(new Label(labels[i]), 0, i);
            grid.add(fields[i], 1, i);
            if (fields[i] instanceof javafx.scene.control.Control control) control.setPrefWidth(300);
        }
        return grid;
    }
    private boolean showForm(String title, GridPane content) {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle(title);
        ButtonType save = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(content);
        return dialog.showAndWait().orElse(ButtonType.CANCEL) == save;
    }
    private <T> StringConverter<T> converter(Function<T, String> toText) {
        return new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : toText.apply(value); }
            @Override public T fromString(String text) { return null; }
        };
    }
    private void info(String text) { new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK).showAndWait(); }
    private void showError(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage() == null ? "Проверьте данные и подключение к базе." : e.getMessage(), ButtonType.OK);
        alert.setHeaderText("Не удалось выполнить операцию"); alert.showAndWait();
    }
}
