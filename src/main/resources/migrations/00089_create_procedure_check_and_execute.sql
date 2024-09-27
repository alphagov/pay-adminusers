--liquibase formatted sql dbms:postgresql splitStatements:false

--changeset uk.gov.pay:create_procedure_check_and_execute
create or replace procedure check_and_execute(
    p_dml_sql text,
    p_table_name text,
    p_expected_no_of_rows_to_update_or_delete numeric
)
language plpgsql
as ' declare
    rows_affected INTEGER;
    total_rows INTEGER;
begin
    EXECUTE format(''SELECT COUNT(*) FROM %I'', p_table_name) INTO total_rows;

    if p_expected_no_of_rows_to_update_or_delete >= total_rows then
        raise exception ''Failed. Expected no. of rows (%) to update/delete can not be same or more than the total number of rows (%) in the table'',
            p_expected_no_of_rows_to_update_or_delete,
            total_rows;
    end if;

    execute p_dml_sql;
    get diagnostics rows_affected = ROW_COUNT;

    if rows_affected != p_expected_no_of_rows_to_update_or_delete then
        raise exception ''Failed. Statement expected to update/delete % rows but updating % rows. Changes not commited.'',
            p_expected_no_of_rows_to_update_or_delete,
            rows_affected;
    end if;

    raise notice ''Success. Statement affected % rows. Expected to update/delete LESS THAN or EQUAL to % rows'',
        rows_affected,
        p_expected_no_of_rows_to_update_or_delete;
end; '

--rollback drop procedure check_and_execute;
