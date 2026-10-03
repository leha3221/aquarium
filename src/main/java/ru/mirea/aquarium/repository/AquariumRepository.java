package ru.mirea.aquarium.repository;
import ru.mirea.aquarium.model.*;
import ru.mirea.aquarium.util.DatabaseManager;
import ru.mirea.aquarium.exception.EntityNotFoundException;
import java.sql.*;
import java.util.*;
public class AquariumRepository {
    public Aquarium create(Aquarium e) {
        try(Connection c=DatabaseManager.getConnection();
            PreparedStatement ps=c.prepareStatement("INSERT INTO aquariums(client_id, name, aquarium_type, volume_liters) VALUES (?, ?, ?, ?)",Statement.RETURN_GENERATED_KEYS)){
            bind(ps,e);
            ps.executeUpdate();
            try(ResultSet rs=ps.getGeneratedKeys()){if(rs.next())e.setId(rs.getLong(1));}
            return e;
        }catch(SQLException ex){throw failure(ex);}
    }
    public List<Aquarium> findAll(){
        try(Connection c=DatabaseManager.getConnection();
            PreparedStatement ps=c.prepareStatement("SELECT * FROM aquariums ORDER BY id");
            ResultSet rs=ps.executeQuery()){
            List<Aquarium> result=new ArrayList<>();
            while(rs.next())result.add(map(rs));
            return result;
        }catch(SQLException ex){throw failure(ex);}
    }
    public Optional<Aquarium> findById(long id){
        try(Connection c=DatabaseManager.getConnection();
            PreparedStatement ps=c.prepareStatement("SELECT * FROM aquariums WHERE id=?")){
            ps.setLong(1,id);
            try(ResultSet rs=ps.executeQuery()){return rs.next()?Optional.of(map(rs)):Optional.empty();}
        }catch(SQLException ex){throw failure(ex);}
    }
    public void update(Aquarium e){
        try(Connection c=DatabaseManager.getConnection();
            PreparedStatement ps=c.prepareStatement("UPDATE aquariums SET client_id=?, name=?, aquarium_type=?, volume_liters=? WHERE id=?")){
            bind(ps,e);ps.setLong(5,e.getId());
            if(ps.executeUpdate()==0)throw new EntityNotFoundException("Aquarium: запись не найдена.");
        }catch(SQLException ex){throw failure(ex);}
    }
    public void delete(long id){
        try(Connection c=DatabaseManager.getConnection();
            PreparedStatement ps=c.prepareStatement("DELETE FROM aquariums WHERE id=?")){
            ps.setLong(1,id);
            if(ps.executeUpdate()==0)throw new EntityNotFoundException("Aquarium: запись не найдена.");
        }catch(SQLException ex){throw failure(ex);}
    }
    private void bind(PreparedStatement ps,Aquarium e)throws SQLException{
        ps.setLong(1, e.getClientId());
        ps.setString(2, e.getName());
        ps.setString(3, e.getType().name());
        ps.setBigDecimal(4, e.getVolumeLiters());
    }
    private Aquarium map(ResultSet rs)throws SQLException{return new Aquarium(rs.getLong("id"),rs.getLong("client_id"), rs.getString("name"), AquariumType.valueOf(rs.getString("aquarium_type")), rs.getBigDecimal("volume_liters"));}
    private RuntimeException failure(SQLException ex){return new RuntimeException("Ошибка БД (Aquarium): "+ex.getMessage(),ex);}
}