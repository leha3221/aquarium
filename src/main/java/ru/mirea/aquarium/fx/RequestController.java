package ru.mirea.aquarium.fx;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import ru.mirea.aquarium.export.CsvExporter;
import ru.mirea.aquarium.model.*;
import ru.mirea.aquarium.repository.ClientRepository;
import ru.mirea.aquarium.repository.ServiceRequestRepository;
import ru.mirea.aquarium.service.ServiceRequestService;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class RequestController {
    @FXML private TableView<ServiceRequest> table;
    @FXML private TextField search;
    @FXML private ComboBox<String> statusFilter, typeFilter, clientFilter;
    @FXML private Spinner<Integer> priorityFilter;
    private final ServiceRequestService service = new ServiceRequestService(new ServiceRequestRepository(), new ClientRepository());
    private List<ServiceRequest> all;

    @FXML private void initialize() {
        addColumn("ID", r -> Long.toString(r.getId()));
        addColumn("Клиент", ServiceRequest::getClientName);
        addColumn("Аквариум", r -> r.getAquariumType().name());
        addColumn("Услуга", r -> r.getServiceType().name());
        addColumn("Описание", ServiceRequest::getDescription);
        addColumn("Статус", r -> r.getStatus().name());
        addColumn("Приоритет", r -> Integer.toString(r.getPriority()));
        addColumn("Плановая дата", r -> r.getScheduledAt().toString().replace('T', ' '));
        addColumn("Цена", r -> r.getPrice().toPlainString());
        statusFilter.setItems(FXCollections.observableArrayList("Все статусы", "NEW", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED"));
        statusFilter.getSelectionModel().selectFirst();
        typeFilter.setItems(FXCollections.observableArrayList("Все типы", "FRESHWATER", "MARINE", "REPTILE", "PLANT"));
        typeFilter.getSelectionModel().selectFirst();
        loadClientFilter();
        priorityFilter.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 1));
        search.textProperty().addListener((o, a, b) -> applyFilters());
        statusFilter.valueProperty().addListener((o, a, b) -> applyFilters());
        typeFilter.valueProperty().addListener((o, a, b) -> applyFilters());
        clientFilter.valueProperty().addListener((o, a, b) -> applyFilters());
        priorityFilter.valueProperty().addListener((o, a, b) -> applyFilters());
        refresh();
    }

    private void addColumn(String title, java.util.function.Function<ServiceRequest, String> value) {
        TableColumn<ServiceRequest, String> c = new TableColumn<>(title);
        c.setCellValueFactory(data -> new SimpleStringProperty(value.apply(data.getValue())));
        if (title.equals("ID") || title.equals("Приоритет") || title.equals("Цена"))
            c.setComparator((left, right) -> new BigDecimal(left).compareTo(new BigDecimal(right)));
        c.setPrefWidth(title.equals("Описание") ? 230 : 120);
        table.getColumns().add(c);
    }
    @FXML private void refresh() { try { all = service.findAll(); loadClientFilter(); applyFilters(); } catch (Exception e) { error(e); } }
    private void applyFilters() {
        if (all == null) return;
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        String status = statusFilter.getValue(), type = typeFilter.getValue(), client = clientFilter.getValue();
        table.setItems(FXCollections.observableArrayList(all.stream().filter(r ->
                (q.isEmpty() || (r.getClientName()+" "+r.getDescription()+" "+r.getServiceType()+" "+r.getId()).toLowerCase(Locale.ROOT).contains(q)) &&
                (status == null || status.equals("Все статусы") || r.getStatus().name().equals(status)) &&
                (type == null || type.equals("Все типы") || r.getAquariumType().name().equals(type)) &&
                (client == null || client.equals("Все клиенты") || r.getClientName().equals(client)) &&
                r.getPriority() >= priorityFilter.getValue()).toList()));
    }
    private void loadClientFilter() {
        List<String> names = new ClientRepository().findAll().stream().map(Client::getFullName).distinct().sorted().toList();
        clientFilter.getItems().setAll("Все клиенты"); clientFilter.getItems().addAll(names);
        clientFilter.getSelectionModel().selectFirst();
    }
    @FXML private void add() { edit(null); }
    @FXML private void editSelected() { ServiceRequest r = table.getSelectionModel().getSelectedItem(); if (r == null) { info("Выберите заявку в таблице."); return; } edit(r); }
    private void edit(ServiceRequest existing) {
        List<Client> clients = new ClientRepository().findAll();
        if (clients.isEmpty()) { info("В базе нет клиентов. Добавьте клиента в таблицу clients и обновите список."); return; }
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle(existing == null ? "Новая заявка" : "Редактирование заявки");
        ButtonType save = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        ComboBox<Client> client = new ComboBox<>(FXCollections.observableArrayList(clients)); client.setConverter(new javafx.util.StringConverter<>() { public String toString(Client c) { return c == null ? "" : c.getFullName()+" (#"+c.getId()+")"; } public Client fromString(String s) { return null; } });
        ComboBox<AquariumType> aquarium = new ComboBox<>(FXCollections.observableArrayList(AquariumType.values()));
        ComboBox<ServiceType> kind = new ComboBox<>(FXCollections.observableArrayList(ServiceType.values()));
        ComboBox<RequestStatus> status = new ComboBox<>(FXCollections.observableArrayList(RequestStatus.values()));
        TextField description = new TextField(), priority = new TextField("3"), scheduled = new TextField(LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString()), price = new TextField("0");
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(9); int row=0;
        for (Object[] item : new Object[][]{{"Клиент",client},{"Тип аквариума",aquarium},{"Услуга",kind},{"Описание",description},{"Статус",status},{"Приоритет (1–5)",priority},{"Плановая дата (yyyy-MM-ddTHH:mm)",scheduled},{"Цена",price}}) { grid.add(new Label((String)item[0]),0,row); grid.add((javafx.scene.Node)item[1],1,row++); }
        if (existing != null) { client.getSelectionModel().select(clients.stream().filter(c -> c.getId()==existing.getClientId()).findFirst().orElse(null)); aquarium.setValue(existing.getAquariumType()); kind.setValue(existing.getServiceType()); status.setValue(existing.getStatus()); description.setText(existing.getDescription()); priority.setText(Integer.toString(existing.getPriority())); scheduled.setText(existing.getScheduledAt().toString()); price.setText(existing.getPrice().toPlainString()); }
        else { client.getSelectionModel().selectFirst(); aquarium.setValue(AquariumType.FRESHWATER); kind.setValue(ServiceType.CLEANING); status.setValue(RequestStatus.NEW); }
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().lookupButton(save).addEventFilter(ActionEvent.ACTION, event -> {
            String problem = validateForm(client.getValue(), aquarium.getValue(), kind.getValue(), description.getText(), status.getValue(), priority.getText(), scheduled.getText(), price.getText());
            if (problem != null) { event.consume(); info(problem); }
        });
        dialog.setResultConverter(button -> button == save ? save : null);
        if (dialog.showAndWait().orElse(null) != save) return;
        try {
            if (client.getValue()==null || aquarium.getValue()==null || kind.getValue()==null || status.getValue()==null) throw new IllegalArgumentException("Заполните все поля со списком.");
            ServiceRequest r = new ServiceRequest(existing == null ? 0 : existing.getId(), client.getValue().getId(), aquarium.getValue(), kind.getValue(), description.getText().trim(), status.getValue(), Integer.parseInt(priority.getText().trim()), existing == null ? LocalDateTime.now() : existing.getCreatedAt(), LocalDateTime.parse(scheduled.getText().trim()), new BigDecimal(price.getText().trim()), client.getValue().getFullName());
            if (existing == null) service.create(r); else service.update(r);
            refresh();
        } catch (Exception e) { error(e); }
    }
    private String validateForm(Client client, AquariumType aquarium, ServiceType kind, String description,
                                RequestStatus status, String priority, String scheduled, String price) {
        if (client == null || aquarium == null || kind == null || status == null) return "Выберите клиента, тип аквариума, услугу и статус.";
        if (description == null || description.isBlank() || description.length() > 500) return "Описание обязательно и должно содержать не более 500 символов.";
        try { int value = Integer.parseInt(priority.trim()); if (value < 1 || value > 5) return "Приоритет должен быть от 1 до 5."; }
        catch (RuntimeException e) { return "Приоритет должен быть целым числом от 1 до 5."; }
        try { LocalDateTime.parse(scheduled.trim()); }
        catch (RuntimeException e) { return "Укажите плановую дату в формате yyyy-MM-ddTHH:mm."; }
        try { if (new BigDecimal(price.trim()).signum() < 0) return "Цена не может быть отрицательной."; }
        catch (RuntimeException e) { return "Цена должна быть числом не меньше нуля."; }
        return null;
    }
    @FXML private void deleteSelected() {
        ServiceRequest r=table.getSelectionModel().getSelectedItem(); if(r==null){info("Выберите заявку в таблице.");return;}
        Alert ask=new Alert(Alert.AlertType.CONFIRMATION,"Удалить заявку №"+r.getId()+"?",ButtonType.YES,ButtonType.NO); ask.setHeaderText("Подтверждение удаления");
        if(ask.showAndWait().orElse(ButtonType.NO)==ButtonType.YES) try { service.delete(r.getId()); refresh(); } catch(Exception e){error(e);}
    }
    @FXML private void showStatistics() {
        if(all==null)return;
        long active=all.stream().filter(r->r.getStatus()==RequestStatus.NEW||r.getStatus()==RequestStatus.CONFIRMED||r.getStatus()==RequestStatus.IN_PROGRESS).count();
        BigDecimal revenue=all.stream().filter(r->r.getStatus()==RequestStatus.COMPLETED).map(ServiceRequest::getPrice).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal average=all.isEmpty()?BigDecimal.ZERO:all.stream().map(ServiceRequest::getPrice).reduce(BigDecimal.ZERO,BigDecimal::add).divide(BigDecimal.valueOf(all.size()),2,java.math.RoundingMode.HALF_UP);
        long high=all.stream().filter(r->r.getPriority()>=4).count(); long overdue=all.stream().filter(r->r.getScheduledAt().isBefore(LocalDateTime.now())&&r.getStatus()!=RequestStatus.COMPLETED&&r.getStatus()!=RequestStatus.CANCELLED).count();
        info("Всего заявок: "+all.size()+"\nАктивных заявок: "+active+"\nЗаявок высокого приоритета: "+high+"\nПросроченных заявок: "+overdue+"\nВыручка по выполненным: "+revenue+"\nСредняя стоимость: "+average);
    }
    @FXML private void openManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/mirea/aquarium/fx/management.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Клиенты, аквариумы и рыбы");
            stage.initOwner(table.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setScene(new Scene(loader.load(), 900, 600));
            stage.showAndWait();
            loadClientFilter();
            refresh();
        } catch (Exception e) { error(e); }
    }
    @FXML private void export() {
        FileChooser chooser=new FileChooser(); chooser.setTitle("Экспорт текущего списка"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)","*.csv")); chooser.setInitialFileName("aquarium-requests.csv");
        java.io.File file=chooser.showSaveDialog(table.getScene().getWindow()); if(file!=null) try { new CsvExporter().export(table.getItems(),Path.of(file.toURI()).toString()); info("Экспортировано строк: "+table.getItems().size()); } catch(Exception e){error(e);}
    }
    private void info(String message){Alert a=new Alert(Alert.AlertType.INFORMATION,message,ButtonType.OK);a.setHeaderText(null);a.showAndWait();}
    private void error(Exception e){Alert a=new Alert(Alert.AlertType.ERROR,e.getMessage()==null?"Проверьте введённые данные и подключение к PostgreSQL.":e.getMessage(),ButtonType.OK);a.setHeaderText("Не удалось выполнить операцию");a.showAndWait();}
}
