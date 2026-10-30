-- Preserve old inspections as individual checklists; new confirmations keep their accepted work guide.
ALTER TABLE lamontana.control_calidad
 ADD COLUMN modalidad_confirmacion varchar(24) NOT NULL DEFAULT 'CHECKLIST'
 CHECK(modalidad_confirmacion IN ('CHECKLIST','CONFORMIDAD_GLOBAL')),
 ADD COLUMN requisitos jsonb;
ALTER TABLE lamontana.control_calidad ADD CONSTRAINT calidad_global_con_requisitos
 CHECK(modalidad_confirmacion <> 'CONFORMIDAD_GLOBAL' OR (requisitos IS NOT NULL AND jsonb_typeof(requisitos)='object'));
