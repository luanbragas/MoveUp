package br.com.moveup.training.application.usecase;

import br.com.moveup.coaching.api.LinkDirectory;
import br.com.moveup.coaching.api.LinkDirectory.LinkRef;
import br.com.moveup.shared.domain.IdGenerator;
import br.com.moveup.shared.domain.ResourceNotFound;
import br.com.moveup.training.application.port.in.ManagePrograms;
import br.com.moveup.training.application.port.in.ManageWorkouts.WorkoutView;
import br.com.moveup.training.application.port.out.ExerciseCatalog;
import br.com.moveup.training.application.port.out.Programs;
import br.com.moveup.training.application.port.out.Workouts;
import br.com.moveup.training.domain.exception.TrainingConflict;
import br.com.moveup.training.domain.model.Program;
import br.com.moveup.training.domain.model.Workout;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ManageProgramsUseCase implements ManagePrograms {

  private final LinkDirectory links;
  private final Programs programs;
  private final Workouts workouts;
  private final ExerciseCatalog catalog;
  private final IdGenerator ids;

  public ManageProgramsUseCase(
      LinkDirectory links,
      Programs programs,
      Workouts workouts,
      ExerciseCatalog catalog,
      IdGenerator ids) {
    this.links = links;
    this.programs = programs;
    this.workouts = workouts;
    this.catalog = catalog;
    this.ids = ids;
  }

  @Override
  @Transactional
  public ProgramView create(UUID professionalId, UUID linkId, ProgramInput input) {
    var link = writableLink(professionalId, linkId);
    var program =
        Program.create(
            ids.newId(),
            link.linkId(),
            link.clientId(),
            input.name(),
            input.goal(),
            input.startsOn(),
            input.endsOn(),
            Program.ScheduleMode.fromCode(input.scheduleMode()),
            input.weeklyTarget());
    programs.archiveActive(link.linkId());
    programs.insert(program);
    return view(program);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ProgramView> active(UUID professionalId, UUID linkId) {
    readableLink(professionalId, linkId);
    return programs.activeFor(linkId).flatMap(programs::find).map(this::view);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramView get(UUID professionalId, UUID programId) {
    var program = programs.find(programId).orElseThrow(ResourceNotFound::new);
    readableLink(professionalId, program.linkId());
    return view(program);
  }

  @Override
  @Transactional
  public ProgramView update(
      UUID professionalId,
      UUID programId,
      int expectedRevision,
      ProgramInput input,
      List<SlotInput> schedule) {
    var program = writableProgram(professionalId, programId);
    program.update(
        expectedRevision,
        input.name(),
        input.goal(),
        input.startsOn(),
        input.endsOn(),
        Program.ScheduleMode.fromCode(input.scheduleMode()),
        input.weeklyTarget(),
        schedule == null
            ? List.of()
            : schedule.stream().map(s -> new Program.Slot(s.workoutId(), s.weekdays())).toList());
    programs.save(program);
    return view(program);
  }

  @Override
  @Transactional
  public WorkoutView addWorkout(UUID professionalId, UUID programId, UUID templateId, String name) {
    var program = writableProgram(professionalId, programId);
    var link = writableLink(professionalId, program.linkId());
    Workout template = null;
    if (templateId != null) {
      template = workouts.find(templateId).orElseThrow(ResourceNotFound::new);
      template.requireOwnedBy(link.organizationId());
    }
    var workout =
        Workout.forProgram(
            ids.newId(), ids.newId(), link.organizationId(), program.id(), template, name);
    workouts.insert(workout, professionalId);
    program.addWorkout(workout.id());
    programs.save(program);
    return WorkoutMapping.view(
        workout, catalog.refs(link.organizationId(), workout.content().exerciseIds()));
  }

  @Override
  @Transactional
  public void archive(UUID professionalId, UUID programId, int expectedRevision) {
    var program = writableProgram(professionalId, programId);
    program.archive(expectedRevision);
    programs.saveArchive(program);
  }

  private Program writableProgram(UUID professionalId, UUID programId) {
    var program = programs.find(programId).orElseThrow(ResourceNotFound::new);
    if (program.archived()) {
      throw new ResourceNotFound();
    }
    writableLink(professionalId, program.linkId());
    return program;
  }

  private LinkRef readableLink(UUID professionalId, UUID linkId) {
    return links.ofProfessional(professionalId, linkId).orElseThrow(ResourceNotFound::new);
  }

  private LinkRef writableLink(UUID professionalId, UUID linkId) {
    var link = readableLink(professionalId, linkId);
    if (!link.acceptsTraining()) {
      throw TrainingConflict.linkNotTrainable();
    }
    return link;
  }

  private ProgramView view(Program p) {
    var stats = programs.workoutStats(p.id());
    var position = new int[] {0};
    var items =
        p.slots().stream()
            .filter(s -> stats.containsKey(s.workoutId()))
            .map(
                s -> {
                  var st = stats.get(s.workoutId());
                  position[0]++;
                  return new ProgramWorkoutView(
                      s.workoutId(),
                      st.name(),
                      position[0],
                      s.weekdays(),
                      st.estimatedMinutes(),
                      st.exercises());
                })
            .toList();
    return new ProgramView(
        p.id(),
        p.linkId(),
        p.name(),
        p.goal(),
        p.startsOn(),
        p.endsOn(),
        p.mode().code(),
        p.weeklyTarget(),
        p.revision(),
        items);
  }
}
