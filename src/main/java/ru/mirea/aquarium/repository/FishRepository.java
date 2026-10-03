package ru.mirea.aquarium.repository;
import ru.mirea.aquarium.model.*;
import ru.mirea.aquarium.util.DatabaseManager;
import ru.mirea.aquarium.exception.EntityNotFoundException;
import java.sql.*;
import java.util.*;
public class FishRepository {
    public Fish create(Fish e) {
        try(Connection c=DatabaseManager.getConnection();
            PreparedStatement ps=c.prepareStatement("INSERT INTO fish(aquarium_id, species, quantity) VALUES (?, ?, ?)",Statement.RETURN_GENERATED_KEYS)){
            bind(ps,e);
            ps.executeUpdate();
            try(ResultSet rs=ps.getGeneratedKeys()){if(rs.next())e.setId(rs.getLong(1));}
            return e;
        }catch(SQLException ex){throw failure(ex);}
    }
    public List<Fish> findAll(){
        try(Connection c=DatabaseManager.getConnection();
            PreparedStatement ps=c.prepareStatement("SELECT * FROM fish ORDER BY id");
            ResultSet rs=ps.executeQuery()){
            List<Fish> result=new ArrayList<>();
            while(rs.next())result.add(map(rs));
            return result;
        }catch(SQLException ex){throw failure(ex);}
    }
    public Optional<Fish> findById(long id){
        try(Connection c=DatabaseManager.getConnection();
            PreparedStatement ps=c.prepareStatement("SELECT * FROM fish WHERE id=?")){
            ps.setLong(1,id);
            try(ResultSet rs=ps.executeQuery()){return rs.next()?Optional.of(map(rs)):Optional.empty();}
        }catch(SQLException ex){throw failure(ex);}
    }
    public void update(Fish e){
        try(Connection c=DatabaseManager.getConnection();
            PreparedStatement ps=c.prepareStatement("UPDATE fish SET aquarium_id=?, species=?, quantity=? WHERE id=?")){
            bind(ps,e);ps.setLong(4,e.getId());
            if(ps.executeUpdate()==0)throw new EntityNotFoundException("Fish: запись не найдена.");
        }catch(SQLException ex){throw failure(ex);}
    }
    public void delete(long id){
        try(Connection c=DatabaseManager.getConnection();
            PreparedStatement ps=c.prepareStatement("DELETE FROM fish WHERE id=?")){
            ps.setLong(1,id);
            if(ps.executeUpdate()==0)throw new EntityNotFoundException("Fish: запись не найдена.");
        }catch(SQLException ex){throw failure(ex);}
    }
    private void bind(PreparedStatement ps,Fish e)throws SQLException{
        ps.setLong(1, e.getAquariumId());
        ps.setString(2, e.getSpecies());
        ps.setInt(3, e.getQuantity());
    }
    private Fish map(ResultSet rs)throws SQLException{return new Fish(rs.getLong("id"),rs.getLong("aquarium_id"), rs.getString("species"), rs.getInt("quantity"));}
    private RuntimeException failure(SQLException ex){return new RuntimeException("Ошибка БД (Fish): "+ex.getMessage(),ex);}
}